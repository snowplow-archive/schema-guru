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

trait SchemaStringHelper extends SchemaHelper {
  /**
   * Holds information about merged JSON Schema String
   *
   * @param formats list of all encountered formats
   */
  private[json] case class StringFieldReducer(formats: List[String]) {
    def getFormat: String = {
      val formatSet = formats.toSet.toList
      if (formatSet.size == 1) formatSet.head
      else "none"
    }
  }

  /**
   * Tries to extract unreduced string field
   * Unreduced state imply it has format field as array
   *
   * @param jField any JValue, but for extraction it need to be
   *               JObject with type string, and format array
   * @return reducer if it is really string field
   */
  private[json] def extractStringField(jField: JValue): Option[StringFieldReducer] = {
    val list: List[StringFieldReducer] = for {
      JObject(field) <- jField
      JField("format", JArray(formats)) <- field
      JField("type", types) <- field
      if (contains(types, "string"))
    } yield StringFieldReducer(formats)
    list.headOption
  }

  /**
   * Eliminates format property if more than one format presented
   *
   * @param original is unreduced JSON Schema with string field
   *                 and format property as JArray in it
   * @return same JValue if string type wasn't or modified JValue otherwise
   */
  def reduceStringFieldFormat(original: JValue) = {
    val stringField = extractStringField(original)
    stringField match {
      case Some(reducer) => original.merge(JObject("format" -> JString(reducer.getFormat)))  // it may be removed further
                                    .removeField { case JField("format", JString("none")) => true
      case _ => false }
      case None => original
    }
  }
}
