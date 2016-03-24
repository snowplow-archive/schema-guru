/*
 * Copyright (c) 2012-2016 Snowplow Analytics Ltd. All rights reserved.
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
package sparkjob

import scalaz._
import Scalaz._

import org.json4s._

import Common.{ SchemaDescription, SchemaVer }
import generators.PredefinedEnums.predefined

/**
 * Class containing all inputs necessary data for schema derivation command
 * Basically, a copy of [[com.snowplowanalytics.schemaguru.cli.SchemaCommand]]
 * with [[input]] and [[output]] as String instead of File and required [[output]]
 */
case class SparkJobCommand private[sparkjob](
  input: String,
  output: String,
  enumCardinality: Int = 0,
  enumSets: Boolean = false,
  vendor: Option[String] = None,
  name: Option[String] = None,
  schemaver: Option[SchemaVer] = None,
  schemaBy: Option[String] = None,
  noLength: Boolean = false,
  ndjson: Boolean = false,
  errorsPath: Option[String] = None) {
  /**
   * Preference representing Schema-segmentation
   * First element of pair is JSON Path, by which we split our Schemas
   * Second element is output path where to put Schemas (can't print it to stdout)
   * None means we don't need to do segmentation
   */
  val segmentSchema = schemaBy.map { jsonPath =>
    (jsonPath, output)
  }

  val selfDescribing: Option[SchemaDescription] = (vendor |@| name) { (v, n) =>
    SchemaDescription(v, n, schemaver.getOrElse(SchemaVer(1,0,0)))
  }

  lazy val successfulEnumSets: List[JArray] =
    if (enumSets) predefined.valuesIterator.toList else Nil
}

