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

// json4s
import org.json4s._

trait SchemaIntegerHelper extends SchemaHelper {
  /**
   * Auxilary class to hold lower and upper bounds for integer ranges
   * @param minimum lower bound
   * @param maximum upper bound
   */
  private[json] case class Range(minimum: BigInt, maximum: BigInt)

  // List of Int ranges sorted by size
  private[json] val ranges = List(
    Range(0, 32767),                Range(-32768, 32767),
    Range(0, 2147483647),           Range(-2147483648, 2147483647),
    Range(0, 9223372036854775807L), Range(-9223372036854775808L, 9223372036854775807L)
  )

  /**
   * Pick [[SchemaIntegerHelper.Range]] for two values
   * Raises [[NoSuchElementException]] if values exceed [[Long]]
   *
   * @param min lower bound
   * @param max upper bound
   * @return [[SchemaIntegerHelper.Range]] for specidied bounds
   */
  private[json] def guessRange(min: BigInt, max: BigInt) =
    ranges.find(r => r.minimum <= min && r.maximum >= max).get

  /**
   * Holds all information about merged JSON Schema Integer
   *
   * @param minimum list of all encountered maximum values
   * @param maximum list of all encountered minimum values
   */
  case class IntegerFieldReducer(minimum: List[BigInt], maximum: List[BigInt]) {
    private val reducedMinimum = minimum.min
    private val reducedMaximum = maximum.max
    private val range = guessRange(reducedMinimum, reducedMaximum)

    /**
     * Minimum bound according to negativeness and size of byte
     */
    val minimumBound: BigInt = range.minimum

    /**
     * Maximum bound according to negativeness and size of byte
     */
    val maximumBound: BigInt = range.maximum
  }

  /**
   * Tries to extract unreduced integer field
   * Unreduced state imply it has minimum and maximum as arrays
   *
   * @param jField any JValue, but for extraction it need to be
   *               JObject with type integer, and minimum/maximum arrays
   * @return reducer if it is really integer field
   */
  def extractIntegerField(jField: JValue): Option[IntegerFieldReducer] = {
    val list: List[IntegerFieldReducer] = for {
      JObject(field) <- jField
      JField("minimum", JArray(minimum)) <- field
      JField("maximum", JArray(maximum)) <- field
      JField("type", types) <- field
      if (contains(types, "integer"))
    } yield IntegerFieldReducer(minimum, maximum)
    list.headOption
  }

  /**
   * Tries to extract all unreduced integer fields and modify minimum/maximum values
   *
   * @param original is unreduced JSON Schema with integer field
   *                 and minimum/maximum fields as JArray in it
   * @return JSON with reduced minimum/maximum properties
   */
  def reduceIntegerFieldRange(original: JValue) = {
    val integerField = extractIntegerField(original)
    integerField match {
      case Some(reducer) => original merge JObject("minimum" -> JInt(reducer.minimumBound),
                                                   "maximum" -> JInt(reducer.maximumBound))
      case None => original
    }
  }
}



