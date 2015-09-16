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
package webui

// Scalaz
import scalaz._
import Scalaz._

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
  def getErrorsAsJson(errors: List[String]): JValue = errors.map(parse(_))

  /**
   * Decide which data format (plain JSON or NDJSON) request contains and
   * parse it with appropriate function
   *
   * @param data from HTTP-request
   * @return list of JSON and errors
   */
  // TODO: implement ndjson
  def getJsonFromRequest(data: MultipartFormData): ValidJsonList = {
    val processed: Seq[ValidJsonList] = for {
      field <- data.fields
    } yield {
        val content = field.entity.data.asString
        field.name match {
          case Some(name) if name.endsWith(".json") => List(parseJson(content, field))
          case Some(name) if name == "enumCardinality" => Nil
          case _ => parseNDJson(content, field)
        }
      }
    processed.toList.flatten
  }

  /**
   * Parse string with one JSON instance
   *
   * @param in string with JSON instance
   * @param field part of request to take file name from
   * @return validation with either JValue or String with JSON error object
   */
  def parseJson(in: String, field: BodyPart): Validation[String, JValue] = {
    try {
      parse(in).success
    } catch {
      case e: JsonParseException => {
        val details: JObject = ("error", "File contents failed to parse into JSON") ~
                               ("message", e.getMessage)
        val error: JObject = field.name match {
          case Some(name) => ("file", name) ~ details
          case None       => ("file", "unknown") ~ details
        }
        compact(error).failure
      }
      case e: Exception => {
        val details: JObject = ("error", "File fetching and parsing failed") ~
                               ("message", e.getMessage)
        val error: JObject = field.name match {
          case Some(name) => ("file", name) ~ details
          case None       => ("file", "unknown") ~ details
        }
        compact(error).failure
      }
    }
  }

  /**
   * Parse string with multiple JSON instances delimited with newline
   *
   * @param in string with JSON instances
   * @param field part of request to take file name from
   * @return list of validations with either JValue or JSON error as String
   */
  def parseNDJson(in: String, field: BodyPart): ValidJsonList = {
    val jsons = for {
      (json, line) <- in.split("\n").zipWithIndex
    } yield {
      try {
        parse(json).success
      } catch {
        case e: JsonParseException => {
          val details: JObject = ("error", s"File contents failed to parse into JSON on line $line") ~
                                 ("message", e.getMessage)
          val error: JObject = field.name match {
            case Some(name) => ("file", name) ~ details
            case None => ("file", "unknown") ~ details
          }
          compact(error).failure
        }
        case e: Exception => {
          val details: JObject = ("error", s"File fetching and parsing failed on line $line") ~
                                 ("message", e.getMessage)
          val error: JObject = field.name match {
            case Some(name) => ("file", name) ~ details
            case None => ("file", "unknown") ~ details
          }
          compact(error).failure
        }
      }
    }
    jsons.toList
  }
}

