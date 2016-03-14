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

// json4s
import org.json4s.JValue

// jsonpath
import io.gatling.jsonpath.JsonPath

// json4s
import org.json4s.jackson.JsonMethods.mapper

object JsonPathExtractor {

  /**
   * Add default values for some exceptional cases and
   * remove all special characters from the string also
   * slice it to 30 chars, so it can be used as file name
   *
   * @param content some optional content to convert
   * @return sliced string without special characters
   */
  private def normalizeLookupResult(content: Option[Any]): Option[String] =
    for {
      k <- content          // unbox
      key <- Option(k)      // check for null
      a = key.toString      // stringify
      if a.trim.length > 0  // check for empty
    } yield a.slice(0, 30).replaceAll("[^a-zA-Z0-9.-]", "_")


  /**
   * Maps content of specified JSON Path to List of JSONs which contains same
   * content
   *
   * @param jsonPath valid JSON Path
   * @param jsons valid non-empty list of JSONs
   * @return Map with content found by JSON Path as key and list of JValues
   *         key is Option where None means used to aggregate already non-valid JSONs
   */
  def segmentByPath(jsonPath: String, jsons: List[JValue]): SegmentedJsons = {
    val schemaToJsons = for {
      json <- jsons
    } yield {
      val jsonObject = mapper.convertValue(json, classOf[Object])
      JsonPath.query(jsonPath, jsonObject) match {
        case Right(iter) =>
          val lookupResult = normalizeLookupResult(iter.toList.headOption)
          lookupResult -> json.success[String]
        case Left(e) => none[String] -> e.reason.failure[JValue]
      }
    }

    // Split successful and failed lookups
    schemaToJsons.foldLeft((List.empty[String], Map.empty[String, List[JValue]])) { (acc, cur) =>
      cur match {
        case (Some(key), Success(json)) => (acc._1, acc._2 |+| Map(key -> List(json)))
        case (None, Failure(fail))      => (s"$fail" :: acc._1, acc._2)
        case _                          => ("Not-string value found" :: acc._1, acc._2)
      }
    }
  }
}

