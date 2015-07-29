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
package com.snowplowanalytics

// Scalaz
import scalaz._
import Scalaz._

// json4s
import org.json4s._

package object schemaguru {
  /**
   * Case Class for containing the result
   * of running SchemaGuru
   */
  case class SchemaGuruResult(schema: JValue, errors: List[String], warning: Option[SchemaWarning] = None)

  /**
   * Type Alias for a Valid list of JSONs
   */
  type ValidJsonList = List[Validation[String, JValue]]

  /**
   * Class holding JSON with file name
   */
  case class JsonFile(fileName: String, content: JValue)

  /**
   * Type Alias for a Valid list of JSON files
   */
  type ValidJsonFileList = List[Validation[String, JsonFile]]
}
