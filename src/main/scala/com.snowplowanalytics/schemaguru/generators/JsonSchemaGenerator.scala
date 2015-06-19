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

// Scala
import scala.annotation.tailrec

// Java
import java.util.UUID
import org.apache.commons.validator.routines.{
  InetAddressValidator,
  UrlValidator
}
import org.joda.time.DateTime

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

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
      case JObject(x) => ("type", "object") ~
                         ("properties", jObjectListProcessor(x)) ~
                         ("additionalProperties", false)
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
          case (k, JString(v))  => List((k, Annotations.annotateString(v)))
          case (k, JInt(v))     => List((k, Annotations.annotateInteger(v)))
          case (k, JDecimal(v)) => List((k, Annotations.annotateDecimal(v)))
          case (k, JDouble(v))  => List((k, Annotations.annotateDouble(v)))
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
          case JString(v)  => Annotations.annotateString(v)
          case JInt(v)     => Annotations.annotateInteger(v)
          case JDecimal(v) => Annotations.annotateDecimal(v)
          case JDouble(v)  => Annotations.annotateDouble(v)
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


  object Annotations {
    def suggestTimeFormat(string: String): Option[String] = {
      if (string.length > 10) { // TODO: find a better way to exclude truncated ISO 8601:2000 values
        try {
          DateTime.parse(string)
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

    def suggestIpFormat(string: String): Option[String] = {
      val validator = new InetAddressValidator()
      if (validator.isValidInet4Address(string)) { Some("ipv4") }
      else if (validator.isValidInet6Address(string)) { Some("ipv6") }
      else { None }
    }

    def suggestUrlFormat(string: String): Option[String] = {
      val urlValidator = new UrlValidator()
      if (urlValidator.isValid(string)) { Some("uri") }
      else { None }
    }

    def suggestBase64Pattern(string: String): Option[String] = {
      val regex = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$"
      if (string.matches(regex)) { Some(regex) }
      else None
    }


    private val formatSuggestions = List(suggestUuidFormat _, suggestTimeFormat _, suggestIpFormat _, suggestUrlFormat _)
    private val patternSuggestions = List(suggestBase64Pattern _)

    /**
     * Tries to guess format of the string
     * If nothing match return "none" format which must be reduced in further transformations
     *
     * @param value is a string we need to recognize
     * @param suggestions list of functions can recognize format
     * @return some format or none if nothing suites
     */
    @tailrec
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
     * TODO: consider one method name with overloaded arguments
     *
     * @return JsonSchemaType with recognized properties
     */
    def annotateString(value: String) =
      JsonSchemaType.StringT ~
      ("format", guessFormat(value, formatSuggestions)) ~
      ("pattern", guessFormat(value, patternSuggestions)) ~
      ("enum", JArray(List(value)))

    /**
     * Set value itself as minimum and maximum for future merge and reduce
     */
    def annotateInteger(value: BigInt) =
      JsonSchemaType.IntegerT ~ ("minimum", value) ~ ("maximum", value)

    /**
     * Set value itself as minimum. We haven't maximum bounds for numbers
     */
    def annotateDecimal(value: BigDecimal) =
      JsonSchemaType.DecimalT ~ ("minimum", value)

    /**
     * Set value itself as minimum. We haven't maximum bounds for numbers
     */
    def annotateDouble(value: Double) =
      JsonSchemaType.DoubleT ~ ("minimum", value)
  }
}

