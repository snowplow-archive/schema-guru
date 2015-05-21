/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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

/**
 * Takes a JSON and converts it into a JsonSchema.
 *
 * TODO List:
 * - Merge the JsonSchema with our BaseSchema
 * - Add ability to pass parameters which we will add to the BaseSchema: description, vendor, name
 * - Add ability to process primitive JSONs: i.e. { "primitive" }
 * - Ensure all matches are exhaustive
 */
object JsonSchemaGenerator {

  // The current directory which we are pulling resources from
  private val BaseSchemaFile = "//vagrant//schema-guru//src//main//resources//base-jsonschema.json"

  /** 
   * Object to contain different
   * types of Json Objects
   */
  private object JsonSchemaType {
    val StringT  = JObject(List(("type", JString("string"))))
    val IntegerT = JObject(List(("type", JString("integer"))))
    val DecimalT = JObject(List(("type", JString("number"))))
    val DoubleT  = JObject(List(("type", JString("number"))))
    val BooleanT = JObject(List(("type", JString("boolean"))))
    val NullT    = JObject(List(("type", JString("null"))))
    val NothingT = JObject(List(("type", JString("null"))))
  }

  /**
   * Will wrap JObjects and JArrays in JsonSchema
   * friendly style and will then begin the processing
   * of the internal lists.
   * Used fot top-level objects and arrays, thus shall not
   * be used for anything other than objects and arrays.
   *
   * @param json the JSON that needs to be processed
   *        into JsonSchema
   * @return the JsonSchema for the JSON
   */
  def jsonToSchema(json: JValue): JValue =
    json match {
      case JObject(x) => ("type", "object") ~ ("properties", jObjectListProcessor(x)) ~ ("additionalProperties", false)
      case JArray(x)  => ("type", "array") ~ ("items", jArrayListProcessor(x))
      case _          => null
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
          case (k, JObject(v))  => List((k, jsonToSchema(JObject(v))))
          case (k, JArray(v))   => List((k, jsonToSchema(JArray(v))))
          case (k, JString(v))  => List((k, Enrichment.enrichString(v)) )
          case (k, JInt(_))     => List((k, JsonSchemaType.IntegerT))
          case (k, JDecimal(_)) => List((k, JsonSchemaType.DecimalT))
          case (k, JDouble(_))  => List((k, JsonSchemaType.DoubleT))
          case (k, JBool(_))    => List((k, JsonSchemaType.BooleanT))
          case (k, JNull)       => List((k, JsonSchemaType.NullT))
          case (k, JNothing)    => List((k, JsonSchemaType.NothingT))
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
          case JString(_)  => JsonSchemaType.StringT
          case JInt(_)     => JsonSchemaType.IntegerT
          case JDecimal(_) => JsonSchemaType.DecimalT
          case JDouble(_)  => JsonSchemaType.DoubleT
          case JBool(_)    => JsonSchemaType.BooleanT
          case JNull       => JsonSchemaType.NullT
          case JNothing    => JsonSchemaType.NothingT
        }
        jArrayListProcessor(xs, (accum ++ List(jType)))
      }
      case Nil => {
        accum match { 
          case list if list.size == 1 => list(0) 
          case list                   => JArray(list)
        }
      }
    }

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
        val exception = e.getMessage
        s"File [$path] contents failed to parse into JSON: [$exception]".failure
      }
      case e: Exception => {
        val exception = e.getMessage
        s"File [$path] fetching and parsing failed: [$exception]".failure
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


  object Enrichment {
    import java.util.UUID
    import org.apache.commons.validator.routines.InetAddressValidator
    import org.joda.time.DateTime.parse

    def suggestTimeFormat(string: String): Option[String] = {
      if (string.length > 10) { // TODO: find a better way to exclude truncated ISO 8601:2000 values
        try {
          parse(string)
          Some("date-time")
        } catch {
          case e: IllegalArgumentException => None
        }
      } else None
    }

    def suggestUuidFormat(string: String): Option[String] = {
      try {
        UUID.fromString(string)
        Some("uuid")
      } catch {
        case e: IllegalArgumentException => None
      }
    }

    def suggestIpFormat(string: String): Option[String]  = {
      val validator = new InetAddressValidator()
      if (validator.isValidInet4Address(string)) Some("ipv4")
      else if (validator.isValidInet6Address(string)) Some("ipv6")
      else None
    }

    private val formatSuggestions = List(suggestUuidFormat _, suggestTimeFormat _, suggestIpFormat _)

    /**
     * Tries to guess format of the string
     *
     * @param value is a string we need to recognize
     * @param suggestions list of functions can recognize format
     * @return some format or none if nothing suites
     */
    def guessFormat(value: String, suggestions: List[String => Option[String]]): Option[String] = {
      suggestions match {
        case Nil => None
        case suggestion :: tail => suggestion(value) match {
          case Some(format) => Some(format)
          case None => guessFormat(value, tail)
        }
      }
    }

    /**
     * Adds properties to string field
     *
     * @return JsonSchemaType with recognized properties
     */
    def enrichString(value: String) = {
      guessFormat(value, formatSuggestions) match {
        case Some(format) => JsonSchemaType.StringT ~ ("format", format)
        case None         => JsonSchemaType.StringT
      }
    }
  }
}

