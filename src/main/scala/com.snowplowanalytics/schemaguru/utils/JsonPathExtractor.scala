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
package utils

// scalaz
import scalaz._
import Scalaz._

// jsonpath
import io.gatling.jsonpath.JsonPath

// json4s
import org.json4s.jackson.JsonMethods.mapper

object JsonPathExtractor {

  type SegmentedJsons = Map[Option[String], ValidJsonList]

  /**
   * Add default values for some exceptional cases and
   * remove all special characters from the string also
   * slice it to 30 chars, so it can be used as file name
   *
   * @param content some optional content to convert
   * @return sliced string without special characters
   */
  private def convertToKey(content: Option[Any]): String = content match {
    case Some(str) =>
      val key = try {
        str.toString
      } catch {
        case _: NullPointerException => "unmatched"
      }
      if (key.trim.length == 0) "unmatched"
      else key.slice(0, 30).replaceAll("[^a-zA-Z0-9.-]", "_")
    case None => "unmatched"
  }
  

  /**
   * Maps content of specified JSON Path to List of JSONs which contains same
   * content
   *
   * @param jsonPath valid JSON Path
   * @param jsonList the validated JSON list
   * @return Map with content found by JSON Path as key and list of JValues
   *         key is Option where None means used to aggregate already non-valid JSONs
   */
  def mapByPath(jsonPath: String, jsonList: List[ValidJson]): SegmentedJsons = {
    val schemaToJsons: List[SegmentedJsons] = for {
      Success(json) <- jsonList
    } yield {
        val jsonObject = mapper.convertValue(json, classOf[Object])
        JsonPath.query(jsonPath, jsonObject) match {
          case Right(iter) => {
            val key = convertToKey(iter.toList.headOption).some
            Map(key -> List(json.success[String]))
          }
          case Left(_) => Map.empty[Option[String], ValidJsonList]
        }
      }

    val failedJsons: List[SegmentedJsons] = for {
      Failure(err) <- jsonList
    } yield Map(none[String] -> List(err.failure))

    (failedJsons ++ schemaToJsons).reduce { (a, b) => a |+| b }
  }
}
