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
  private[json] case class StringFieldReducer(formats: List[String], patterns: List[String]) {
    def getFormat: String = {
      val formatSet = formats.toSet.toList
      if (formatSet.size == 1) { formatSet.head }
      else { "none" }
    }

    def getPattern: String = {
      val patternSet = patterns.toSet.toList
      if (patternSet.size == 1) { patternSet.head }
      else { "none" }
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
  private[json] def extractStringWithFormat(jField: JValue): Option[StringFieldReducer] = {
    val list: List[StringFieldReducer] = for {
      JObject(field) <- jField
      JField("format", JArray(formats)) <- field
      JField("type", types) <- field
      if (contains(types, "string"))
    } yield StringFieldReducer(formats, Nil)
    list.headOption
  }
  /**
   * Tries to extract unreduced string field
   * Unreduced state imply it has pattern field as array
   *
   * @param jField any JValue, but for extraction it need to be
   *               JObject with type string, and pattern array
   * @return reducer if it is really string field
   */
  private[json] def extractStringWithPattern(jField: JValue): Option[StringFieldReducer] = {
    val list: List[StringFieldReducer] = for {
      JObject(field) <- jField
      JField("pattern", JArray(patterns)) <- field
      JField("type", types) <- field
      if (contains(types, "string"))
    } yield StringFieldReducer(Nil, patterns)
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
    val stringField = extractStringWithFormat(original)
    stringField match {
      case Some(reducer) => original.merge(JObject("format" -> JString(reducer.getFormat)))  // it may be removed further
                                    .removeField { case JField("format", JString("none")) => true
                                                   case _ => false }
      case None => original
    }
  }

  /**
   * Eliminates pattern property if more than one format presented
   *
   * @param original is unreduced JSON Schema with string field
   *                 and pattern property as JArray in it
   * @return same JValue if string type wasn't or modified JValue otherwise
   */
  def reduceStringFieldPattern(original: JValue) = {
    val stringField = extractStringWithPattern(original)
    stringField match {
      case Some(reducer) => original.merge(JObject("pattern" -> JString(reducer.getPattern)))  // it may be removed further
                                    .removeField { case JField("pattern", JString("none")) => true
                                                   case _ => false }
      case None => original
    }
  }
}
