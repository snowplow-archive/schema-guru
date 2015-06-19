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
import org.json4s.JsonDSL._

// This library
import JValueImplicits._

/**
 * Holds information about merged JSON Schema String
 *
 * @param format list of all encountered formats
 * @param pattern list of all encountered patterns
 * @param `type` must be always "string" (or contain "string"), or it won't be mapped
 */
// TODO: find out why it don't work when inserted into trait
private[json] case class StringFieldReducer(format: JValue, pattern: JValue, `type`: JValue) extends SchemaHelper {
  if (!contains(`type`, "string")) throw new MappingException("Does not contain type string")

  def getField(field: String): Option[String] = field match {
    case "format"  => getProps(format)
    case "pattern" => getProps(pattern)
    case _         => None
  }

  private[json] def getProps(props: JValue): Option[String] = props match {
    case JArray(fms) if fms.toSet.size == 1 =>
      fms.headOption flatMap {
        case JString(s) => Some(s)
        case _          => None
      }
    case JString(s) => Some(s)
    case _          => None
  }
}

trait SchemaStringHelper extends SchemaHelper {
  /**
   * Tries to extract unreduced string field
   * Unreduced state imply it may have format and pattern fields as arrays
   *
   * @param jField any JValue, but for extraction it need to be
   *               JObject with type string, and format array
   * @return reducer if it is really string field
   */
  def extractString(jField: JValue) =
      jField.extractOpt[StringFieldReducer]

  /**
   * Eliminates pattern property if more than one format presented
   *
   * @param field is either "field" or "format" since they have same reduce
   *              strategy
   * @param original is unreduced JSON Schema with string field
   *                 and pattern property as JArray in it
   * @return same JValue if string type wasn't or modified JValue otherwise
   */
  def reduceStringField(field: String)(original: JValue) = {
    val stringField = extractString(original)
    stringField match {
      case None          => original    // it's not a string field
      case Some(reducer) => reducer.getField(field) match {
        case None          => original removeKey(field)
        case Some(pattern) => original merge((field, pattern): JObject)   // set new pattern
      }
    }
  }
}
