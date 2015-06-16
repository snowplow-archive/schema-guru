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
import scalaz._
import Scalaz._

// This library
import generators.{
  JsonSchemaGenerator => JSG,
  JsonSchemaMerger => JSM
}

// json4s
import org.json4s._
import org.json4s.JsonDSL._

object SchemaGuru {
  /**
   * Takes the valid list of JSONs and returns the JsonSchema
   * Core function of Schema Guru
   *
   * @param list The Validated JSON list
   * @return the final JsonSchema
   */
  // TODO: Turn JsonSchemaGenerator into Akka Actor
  def convertsJsonsToSchema(list: ValidJsonList): SchemaGuruResult = {

    // TODO: Throw error if goodJsons list is Nil
    val goodJsons = for {
      Success(json) <- list
    } yield {
      JSG.jsonToSchema(json)
    }

    val badJsons = for {
      Failure(err) <- list
    } yield err

    val schema = JSM.mergeJsonSchemas(goodJsons)  // Schema with aux info

    val (finalSchema, warning) = SchemaWarning.splitSchemaAndWarnings(schema)

    SchemaGuruResult(finalSchema, badJsons.toList, warning)
  }
}
