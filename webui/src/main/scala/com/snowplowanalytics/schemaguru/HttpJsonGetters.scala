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

// Scalaz
import scalaz.Scalaz._
import scalaz._

// Spray
import spray.http._

// json
import com.fasterxml.jackson.core.JsonParseException
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 * Contain methods reponsible for extracting JSON from HTTP request
 */
trait HttpJsonGetters {
  // TODO: find a better way, add possible failure
  def getErrorsAsJson(errors: List[String]): JValue =
    errors.map(parse(_))

  def getJsonFromRequest(data: MultipartFormData): ValidJsonList = {
    val proccessed = for {
      field <- data.fields
    } yield {
        try {
          val content = field.entity.data.asString
          parse(content).success
        } catch {
          case e: JsonParseException => {
            val details: JObject = ("error", "File contents failed to parse into JSON") ~ ("message", e.getMessage)
            val error: JObject = field.name match {
              case Some(name) => ("file", name) ~ details
              case None       => ("file", "unknown") ~ details
            }
            compact(error).failure
          }
          case e: Exception => {
            val details: JObject = ("error", "File fetching and parsing failed") ~ ("message", e.getMessage)
            val error: JObject = field.name match {
              case Some(name) => ("file", name) ~ details
              case None       => ("file", "unknown") ~ details
            }
            compact(error).failure
          }
        }
      }
    proccessed.toList
  }
}

