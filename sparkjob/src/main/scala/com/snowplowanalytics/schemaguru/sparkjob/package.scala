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

// Spark
import org.apache.spark.rdd.RDD

// This library
import schema.JsonSchema

package object sparkjob {
  /**
   * Copy of schemaguru.JsonConvertResult written for RDD
   *
   * @param schemas RDD with micro-schemas derived from each instance
   * @param errors RDD with errors happened during deriving of micro-schemas
   */
  case class JsonConvertResultRDD(schemas: RDD[JsonSchema], errors: RDD[String])

  /**
   * Copy of schemaguru.SchemaGuruResult written for RDD
   *
   * @param schema schema after reduce
   * @param errors errors encountered due derive and merge operations
   */
  case class SchemaGuruResultRDD(schema: JsonSchema, errors: RDD[String], warnings: List[SchemaWarning])
}
