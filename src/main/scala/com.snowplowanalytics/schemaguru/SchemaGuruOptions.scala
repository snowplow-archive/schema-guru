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

// json4s
import org.json4s._

// This library
import generators.PredefinedEnums.predefined

/**
 * Class representing all options passed to Guru from any kind of UI
 *
 * @param input path to input directory or file
 * @param output path to output directory of file
 * @param enumCardinality maximum enum cardinality
 * @param ndjson process newline-delimited JSON
 * @param schemaBy JSON Path to Schema name for segmenting instances
 * @param enumSets list of predefined enum sets
 * @param segmentSchema pair of schema name and dir for segment schema output
 * @param selfDescribing info for self-describing
 * @param errorsPath path to output errors
 */
// TODO: use it on CLI and Web UI
// TODO: merge with SchemaContext
case class SchemaGuruOptions(
  input: String,
  output: Option[String],
  enumCardinality: Int,
  ndjson: Boolean,
  schemaBy: Option[String],
  enumSets: List[String],
  segmentSchema: Option[(String, String)],    // JsonPath, DirPath
  selfDescribing: Option[SelfDescribingSchema],
  errorsPath: Option[String] = None,
  noLength: Boolean = false
) {
  /**
   * Get validated list of enums predefined in
   * `generators.PredefinedEnums.predefined` and in filesystem
   */
  lazy val getEnumSets: Seq[Validation[String, JArray]] = {
    if (enumSets.contains("all")) { predefined.values.map(_.success).toList }
    else enumSets.map { key =>
      if (predefined.isDefinedAt(key)) predefined(key).success
      else key.failure
    }
  }
}
