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
package json

// Scala
import scala.language.implicitConversions

// json4s
import org.json4s._
import org.json4s.JsonDSL._

trait SchemaHelper {
  implicit val formats = DefaultFormats // Brings in default date formats etc.

  /**
   * Filters and extracts list of Ints from List of JValues
   */
  implicit def extractIntsFromJValues(jValues: List[JValue]): List[BigInt] =
    for (jValue <- jValues; JInt(int) <- jValue) yield int

  /**
   * Filters and extracts list of Doubles and Ints (as Doubles) from List of JValues
   */
  implicit def extractNumericFromJValues(jValues: List[JValue]): List[Double] = {
    val doubles = for (jValue <- jValues; JDouble(double) <- jValue) yield double
    val ints = for (jValue <- jValues; JInt(int) <- jValue) yield int.toDouble
    doubles ++ ints
  }

  /**
   * Filters and extracts list of Strings from List of JValues
   */
  implicit def extractStringsFromJValues(jValues: List[JValue]): List[String] =
    for (jValue <- jValues; JString(str) <- jValue) yield str

  /**
   * Some types in JSON Schema can be presented as product types
   * e.g. "type": ["string", "number"].
   * This predicate detects if current field contains type we're looking for
   * @param value is possible value with JArray, JString or any other JValue
   * @param string string we're looking for. Possible one of schema types
   */
  def contains(value: JValue, string: String) = value match {
    case JString(content) if (content == string) => true
    case JArray(types) if types.contains(JString(string)) => true
    case _ => false
  }

  /**
   * Some types in JSON Schema can be presented as product types
   * e.g. "type": ["string", "number"].
   * This predicate detects if current object contains type we're looking for
   * @param value is JSON. Only JObject can contain type
   * @param `type` one of schema types
   */
  def containsType(value: JValue, `type`: String): Boolean = value match {
    case o @ JObject(_) => o.findField {
      case ("type", types) => contains(types, `type`)
      case _ => false
    } match {
      case Some(_) => true
      case None => false
    }
    case _ => false
  }
}

object SchemaHelpers extends SchemaIntegerHelper with SchemaNumberHelper with SchemaStringHelper
