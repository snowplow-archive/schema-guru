/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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
package generators

// Scalaz
import scalaz._
import Scalaz._

// Jackson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.core.JsonParseException

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.scalaz.JsonScalaz._

/**
 * Takes a list of JsonSchemas and merges them together into
 * a master schema which will validate any of the sub schemas.
 */
object JsonSchemaMerger {

  /**
   * Merges a list of JsonSchemas together into one
   *
   * @param jsonSchemaList The list of Schemas which
   *        we want to merge
   * @return the cumulative JsonSchema
   */
  def mergeJsonSchemas(jsonSchemaList: List[JValue], accum: JValue = Nil): JValue =
    jsonSchemaList match {
      case x :: xs => mergeJsonSchemas(xs, formatSchemaForMerge(x).merge(accum))
      case Nil     => formatMergedSchema(accum)
    }

  /**
   * Transforms the type descriptor for every key
   * in the JsonSchema into an array so that we
   * can merge JsonSchemas together.
   * 
   * @param jsonSchema The JsonSchema which we are
   *        preparing for a merge
   * @return the formatted JsonSchema ready for merge
   */
  def formatSchemaForMerge(jsonSchema: JValue): JValue =
    jsonSchema transformField {
      case ("type", JObject(v)) => ("type", JObject(v))
      case ("type", v)          => ("type", JArray(List(v)))
    }

  /**
   * Cleans up instances where the type descriptor
   * for a key has an array of length 1.
   *
   * i.e. "type" : ["string"] -> "type" : "string"
   *
   * @param jsonSchema The Schema we now want to clean
   * @return the formatted JsonSchema ready for publishing
   */
  def formatMergedSchema(jsonSchema: JValue): JValue =
    jsonSchema transformField {
      case ("type", JArray(list))  => {
        ("type", list match {
          case list if list.size == 1 => list(0)
          case list                   => JArray(list)
        })
      }
    }
}
