/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.schemaguru
package generators

// Scalaz
import scalaz._
import Scalaz._

// Jackson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.core.JsonParseException

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.scalaz.JsonScalaz._
import org.json4s.{ Extraction, NoTypeHints }
import org.json4s.JsonDSL.WithDouble._
import org.json4s.jackson.Serialization

/**
 * Takes a JSON and converts it into a JsonSchema.
 *
 * TODO List:
 * - Merge the JsonSchema with our BaseSchema
 * - Add ability to pass parameters which we will add to the BaseSchema: description, vendor, name
 * - Add ability to process primitive JSONs: i.e. { "primitive" }
 * - I am sure there are more things...
 */
object JsonSchemaGenerator {

  // The current directory which we are pulling resources from
  private val BaseSchemaFile = "//vagrant//schema-guru//src//main//resources//base-jsonschema.json"

  // The pointer nested inside some JArrays
  private val FakeKeyPointer = JString("type: name-value")

  // Needed for re-encoding types
  implicit val formats = Serialization.formats(NoTypeHints)

  /**
   * Will wrap JObjects and JArrays in JsonSchema
   * friendly style and will then begin the processing
   * of the internal lists.
   *
   * @param json the JSON that needs to be processed
   *        into JsonSchema
   * @return the JsonSchema for the JSON
   */
  def jsonToSchema(json: JValue): JValue =
    json match {
      case JObject(x) => JObject(List(("type", JString("object")), ("properties", JObject(jObjectListProcessor(x))), ("additionalProperties", JBool(false))))
      case JArray(x)  => JObject(List(("type", JString("array")), ("items", jArrayListProcessor(x))))
    }

  /**
   * This function processes a list of elements pulled from
   * a JObject recursively until all elements are accounted
   * for.
   *
   * @param jObjectList is the List of elements pulled from
   *        a JObject in the JSON we are processing.
   * @param accum is the accumulated list of (key, JValues) 
   *        we have which will make this function tail recursive
   * @return the contents of the processed JObject list
   */
  def jObjectListProcessor(jObjectList: List[(String, JValue)], accum: List[(String, JValue)] = List()): List[(String, JValue)] =
    jObjectList match {
      case x :: xs => {
        val jSchema: List[(String, JValue)] = x match {
          case (k, JObject(v)) => List((k, encodeJson(jsonToSchema(JObject(v)))))
          case (k, JArray(v))  => {
            List((k, encodeJson(v match {
              case list if list.contains(FakeKeyPointer) => JObject(List(("type", jArrayListToTypes(remove(FakeKeyPointer, list)))))
              case list => JObject(List(("type", JString("array")), ("items", jArrayListProcessor(list))))
            })))
          }
        }
        jObjectListProcessor(xs, (accum ++ jSchema))
      }
      case Nil => accum
    }

  /**
   * This function processes a list of elements pulled from
   * a JArray recursively until all elements are accounted
   * for.
   *
   * @param jArrayList is the List of elements pulled from
   *        a JArray in the JSON we are processing.
   * @param accum is the accumulated list of JValues we have
   *        which will make this function tail recursive
   * @return the contents of the processed JArray list
   */
  def jArrayListProcessor(jArrayList: List[JValue], accum: List[JValue] = List()): JValue =
    jArrayList match {
      case x :: xs => {
        val jType = x match {
          case JObject(v)  => jsonToSchema(JObject(v))
          case JArray(v)   => jsonToSchema(JArray(v))
          case JString(_)  => JObject(List(("type", JString("string"))))
          case JInt(_)     => JObject(List(("type", JString("integer"))))
          case JDecimal(_) => JObject(List(("type", JString("number"))))
          case JDouble(_)  => JObject(List(("type", JString("number"))))
          case JBool(_)    => JObject(List(("type", JString("boolean"))))
          case JNull       => JObject(List(("type", JString("null"))))
        }
        jArrayListProcessor(xs, (accum ++ List(jType)))
      }
      case Nil => {
        accum match { 
          case list if list.size == 1 => list(0) 
          case list if list.size > 1  => JArray(list) 
        }
      }
    }

  /**
   * Takes a list of JValues which we know will only contain
   * the types listed and converts it to a list of JValues
   * which will describe all potential types for a key.
   *
   * @param jValueList the JValue list of entries which we
   *        will need to convert.
   * @param accum is the accumulated list of JValues we have
   *        which will make this function tail recursive
   * @return a JValue containing either a single entry or 
   *         a JArray list of entries.
   */
  def jArrayListToTypes(jValueList: List[JValue], accum: List[JValue] = List()): JValue =
    jValueList match {
      case x :: xs => {
        val jType = x match {
          case JString(_)  => JString("string")
          case JInt(_)     => JString("integer")
          case JDecimal(_) => JString("number")
          case JDouble(_)  => JString("number")
          case JBool(_)    => JString("boolean")
          case JNull       => JString("null")
        }
        jArrayListToTypes(xs, (accum ++ List(jType)))
      }
      case Nil => {
        accum match { 
          case list if list.size == 1 => list(0)
          case list if list.size > 1  => JArray(list)
        }
      }
    }

  /**
   * Used for changing JObject types to JValue
   *
   * @param src Is AnyRef currently but only being
   *        used to re-encode JObjects
   * @return the re-encoded JObject as a JValue
   */
  def encodeJson(src: AnyRef): JValue = 
    Extraction.decompose(src)

  /**
   * Removes a JValue from a List of JValues
   * 
   * @param value The JValue we want to remove
   *        eg: JString("subscribe")
   * @param list The List of JValues we are going
   *        attempt to remove from
   * @return the list minus the value if was found
   *         or the original list
   */
  def remove(value: JValue, list: List[JValue]) = 
    list diff List(value)

  /**
   * Returns our self-describing schema file.
   * This is just a barebones layout for how it 
   * should be structured.
   *
   * @param path The full path to the file we want
   *        to use as the base
   * @return a validated JValue containing the contents
   *         of the file we specified
   */
  def getBaseSchemaFile(path: String = BaseSchemaFile): Validation[String, JValue] =
    try {
      val file = scala.io.Source.fromFile(path)
      val content = file.mkString
      parse(content).success
    } catch {
      case e: JsonParseException => {
        val exception = e.toString
        s"File [$path] contents failed to parse into JSON: [$exception]".fail
      }
      case e: Exception => {
        val exception = e.toString
        s"File [$path] fetching and parsing failed: [$exception]".fail
      }
    }

  /**
   * Fills the self-describing JsonSchema parameters
   *
   * @param rawSchema is the base schema we are going to
   *        add values for
   * @param description The schemas description; example:
   *        'Schema for a MailChimp subscribe event'
   * @param vendor The vendor name for this schema; example:
   *        'com.mailchimp'
   * @param name The name of the json this schema describes
   *        example: 'subscribe'
   * @return the updated jsonschema containing these values
   */
  def addSelfDescOpts(rawSchema: JValue, description: String, vendor: String, name: String): JValue = 
    rawSchema transformField {
      case ("description", JString(_)) => ("description", JString(description))
      case ("vendor", JString(_))      => ("vendor", JString(vendor))
      case ("name", JString(_))        => ("name", JString(name))
    }
}
