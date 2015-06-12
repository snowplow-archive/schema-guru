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

import org.json4s._

trait SchemaNumberHelper extends SchemaHelper {
  /**
   * Holds information about merged JSON Schema Number
   *
   * @param minimums list of all encountered maximum values
   */
  private[json] case class NumberFieldReducer(minimums: List[Double]) {
    /**
     * Check if field can be less than zero
     */
    def isNegative: Boolean = minimums.min < 0
  }

  /**
   * Tries to extract unreduced number field
   * Unreduced state imply it has minimum field as array
   *
   * @param jField any JValue, but for extraction it need to be
   *               field with JObject as value with type number, and minimum array
   * @return reducer if it is really number field
   */
  private[json] def extractNumberField(jField: JValue): Option[NumberFieldReducer] =  {
    val list: List[NumberFieldReducer] = for {
      JObject(field) <- jField
      JField("minimum", JArray(minimum)) <- field
      JField("type", types) <- field
      if (contains(types, "number"))
    } yield NumberFieldReducer(minimum)
    list.headOption
  }

  /**
   * Eliminates maximum property possible left by merge with integer
   * and minimum property if number can be negative (otherwise set it to 0)
   *
   * @param original is unreduced JSON Schema with number field
   *                 and minimum field as JArray in it
   * @return same JValue if number type wasn't or modified JValue otherwise
   */
  def reduceNumberFieldRange(original: JValue): JValue = {
    val numberField = extractNumberField(original)
    numberField match {
      case Some(reducer) => original.merge(JObject("minimum" -> JInt(0)))  // it may be removed further
                                    .removeField { case JField("minimum", _) => reducer.isNegative
                                                   case JField("maximum", _) => true
                                                   case _ => false }
      case None => original
    }
  }

  /**
   * Check if field type contains both integer and number
   *
   * @param types array of types
   * @return boolean indicating whether it is merged value
   */
  def isMergedNumber(types: List[JValue]): Boolean =
    types.contains(JString("integer")) && types.contains(JString("number"))
}
