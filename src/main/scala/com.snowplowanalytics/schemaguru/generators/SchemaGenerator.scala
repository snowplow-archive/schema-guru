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

import com.snowplowanalytics.schemaguru.schema.types._

import scalaz._
import Scalaz._

// Scala
import scala.annotation.tailrec

// Java
import java.util.UUID

import org.apache.commons.validator.routines.{InetAddressValidator, UrlValidator}
import org.joda.time.DateTime

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods.compact

// This library
import schema._
import Helpers._

/**
 * Takes a JSON and converts it into a JsonSchema.
 *
 * TODO List:
 * - Add ability to process primitive JSONs: i.e. { "primitive" }
 */
class SchemaGenerator(implicit val context: SchemaContext) extends Serializable {

  /**
   * Validate that on top-level this JSON instance is object or array and
   * start to recursively convert it to JSON Schema
   *
   * @param json content of JSON file that needs to be process
   * @return validated micro-schema (Schema for a single value) or error
   *         message if schema can't be derived for this value
   */
  def jsonToSchema(json: JValue): Validation[String, JsonSchema] =
    json match {
      case JObject(x) => subJsonToSchema(json).success
      case JArray(x)  => subJsonToSchema(json).success
      case _          => s"JSON instances must contain only objects or arrays. ${compact(json).slice(0, 32)} is unacceptable".failure
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
  private def subJsonToSchema(json: JValue): JsonSchema =
    json match {
      case JObject(x) => ObjectSchema(jObjectListProcessor(x).toMap)
      case JArray(x)  => jArrayListProcessor(x)
      case _          => null // will never happen
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
  def jObjectListProcessor(jObjectList: List[(String, JValue)], accum: List[(String, JsonSchema)] = Nil): List[(String, JsonSchema)] =
    jObjectList match {
      case x :: xs => {
        val jSchema: List[(String, JsonSchema)] = x match {
          case (k, JObject(v))  => List((k, subJsonToSchema(JObject(v))))
          case (k, JArray(v))   => List((k, subJsonToSchema(JArray(v))))
          case (k, JString(v))  => List((k, Annotations.annotateString(v)))
          case (k, JInt(v))     => List((k, Annotations.annotateInteger(v)))
          case (k, JDecimal(v)) => List((k, Annotations.annotateNumber(v)))
          case (k, JDouble(v))  => List((k, Annotations.annotateNumber(v)))
          case (k, JBool(_))    => List((k, BooleanSchema()))
          case (k, JNull)       => List((k, NullSchema()))
          case (k, JNothing)    => List((k, NullSchema()))  // should never appear
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
  def jArrayListProcessor(jArrayList: List[JValue], accum: List[JsonSchema] = List()): ArraySchema =
    jArrayList match {
      case x :: xs => {
        val jType = x match {
          case JObject(v)  => subJsonToSchema(JObject(v))
          case JArray(v)   => subJsonToSchema(JArray(v))
          case JString(v)  => Annotations.annotateString(v)
          case JInt(v)     => Annotations.annotateInteger(v)
          case JDecimal(v) => Annotations.annotateNumber(v)
          case JDouble(v)  => Annotations.annotateNumber(v)
          case JBool(_)    => BooleanSchema()
          case JNull       => NullSchema()
          case JNothing    => NullSchema()
        }
        jArrayListProcessor(xs, (accum ++ List(jType)))
      }
      case Nil => {
        accum match {
          case list if list.size == 1 => ArraySchema(list.head)
          case list                   => {
            // here we can produce tuple validation see #101
            // or may be it is better to not merge (suml) array elements
            // and left it as is for further steps?

            implicit val monoid = getMonoid(context)

            ArraySchema(list.suml)
          }
        }
      }
    }

  /**
   * Annotations are properties of schema derived from one value
   * eg. "127.0.0.1" -> {maxLength: 9, format: ipv4, enum: ["127.0.0.1"]}
   *     33          -> {minimum: 33, maximum: 33, enum: [33]}
   */
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
      context.quantity match {
        case Some(quantity) if quantity < 10 && string.length < 32 => None  // don't apply suggestion on small instance set
        case _                               => {
          val regex = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$"
          if (string.matches(regex)) { Some(regex) }
          else None
        }
      }
    }

    private val formatSuggestions = List(suggestUuidFormat _, suggestTimeFormat _, suggestIpFormat _, suggestUrlFormat _)
    private val patternSuggestions = List(suggestBase64Pattern _)

    /**
     * Tries to guess format or pattern of the string
     * If nothing match return "none" format which must be reduced in further transformations
     *
     * @param value is a string we need to recognize
     * @param suggestions list of functions can recognize format
     * @return some format or none if nothing suites
     */
    @tailrec
    def suggestAnnotation(value: String, suggestions: List[String => Option[String]]): Option[String] = {
      suggestions match {
        case Nil => None
        case suggestion :: tail => suggestion(value) match {
          case Some(format) => Some(format)
          case None => suggestAnnotation(value, tail)
        }
      }
    }

    /**
     * Construct enum only if it is in predefined enum set or enum
     * cardinality > 0
     *
     * @param enumValue JSON value
     * @return same value wrapped in Option
     */
    def constructEnum(enumValue: JValue): Option[List[JValue]] = {
      lazy val inEnum: Boolean = context.inOneOfEnums(enumValue)
      if (context.enumCardinality == 0 && context.enumSets.isEmpty) {
        None
      } else if (context.enumCardinality > 0 || inEnum) {
        Some(List(enumValue))
      } else {
        None
      }
    }

    // TODO: consider one method name with overloaded argument types
    /**
     * Adds properties to string field
     */
    def annotateString(value: String): StringSchema = {
      StringSchema(
        suggestAnnotation(value, formatSuggestions),
        suggestAnnotation(value, patternSuggestions),
        minLength = if (context.deriveLength) Some(value.length) else None,
        maxLength = if (context.deriveLength) Some(value.length) else None,
        enum = constructEnum(JString(value))
      )
    }

    /**
     * Set value itself as minimum and maximum for future merge and reduce
     * Add itself to enum array
     */
    def annotateInteger(value: BigInt) =
      IntegerSchema(Some(value.toLong), Some(value.toLong), constructEnum(JInt(value)))

    /**
     * Set value itself as minimum. We haven't maximum bounds for numbers
     * Add itself to enum array
     */
    def annotateNumber(value: BigDecimal) =
      NumberSchema(value.toDouble.some, value.toDouble.some, constructEnum(JDouble(value.toDouble)))

    /**
     * Set value itself as minimum. We haven't maximum bounds for numbers
     * Add itself to enum array
     */
    def annotateNumber(value: Double) =
      NumberSchema(value.some, value.some, constructEnum(JDouble(value)))
  }
}

object SchemaGenerator {
  def apply(implicit schemaContext: SchemaContext) = new SchemaGenerator
}
