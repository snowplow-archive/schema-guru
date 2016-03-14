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
import org.json4s.JsonDSL._

// This library
import schemaguru.schema.JsonSchema

package object schemaguru {
  /**
   * Class for containing the result of running instance-to-microschema
   * conversion
   *
   * @param schemas contain list of micro-schemas, derived from single
   *                JSON instances. These schemas will validate only single
   *                value against which they were derived. For eg. they contain
   *                enum or min/max properties with single value
   * @param errors list of error messages for all parsed instances, whether it's
   *               invalid JSON or non-object/array value
   */
  case class JsonConvertResult(schemas: List[JsonSchema], errors: List[String])

  /**
   * Class for containing the result of running SchemaGuru.
   * Next processing step after ``JsonConvertResult``
   *
   * @param schema merged and transformed JSON Schema ready for use
   * @param errors list of all fatal errors encountered during previous steps
   * @param warning list of all warnings (non-fatal) encountered during
   *                previous steps
   */
  case class SchemaGuruResult(schema: Schema, errors: List[String], warning: Option[SchemaWarning] = None) {

    /**
     * Add some errors post-factum
     *
     * @param newErrors list of errors
     * @return same result with new errors appended
     */
    def appendErrors(newErrors: List[String]): SchemaGuruResult =
      this.copy(errors = errors ++ newErrors)

    /**
     * Describe containing `schema` with provided information
     * Doesn't do anything if vendor or name is None
     *
     * @param vendor optional vendor
     * @param name optional name
     * @param version version, will be 1-0-1 by default
     * @return modified SchemaGuru result
     */
    def describe(vendor: Option[String], name: Option[String], version: Option[String]): SchemaGuruResult = {
      val description = (vendor |@| name) { (v, n) =>
        Description(v, n, version.getOrElse("1-0-0"))
      }
      this.copy(schema = schema.copy(description = description))
    }
  }

  /**
   * Class for containing both resulting schema and self-describing info
   *
   * @param schema ready for use JSON Schema object
   * @param description optional self-describing information
   */
  case class Schema(schema: JsonSchema, description: Option[Description]) {

    /**
     * Represent result as JSON
     *
     * @return JSON Object with resulting schema
     */
    def toJson: JObject = description match {
      case Some(info) => {
        val uri: JObject = ("$schema", Description.uri)
        val selfObject: JObject = ("self",
          (("vendor", info.vendor): JObject) ~
          (("name", info.name): JObject) ~
          (("version", info.version): JObject) ~
           ("format", "jsonschema"))

        uri.merge(selfObject).merge(schema.toJson)
      }
      case None => schema.toJson
    }
  }

  /**
   * Used to annotate schema with data for self-describing schema
   *
   * @param vendor vendor of schema
   * @param name name of schema (parameter isn't optional)
   * @param version optional version
   */
  case class Description(vendor: String, name: String, version: String)

  object Description {
    val uri = "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#"
  }

  /**
   * Result of JSON segmentation
   * First element contains list of errors
   * Second element is Map, where
   * key is Schema name found by JSON path and value is list of JSONs
   */
  type SegmentedJsons = (List[String], Map[String, List[JValue]])

  /**
   * Type alias for a Valid JSON Schema
   */
  type ValidSchema = Validation[String, JsonSchema]

  /**
   * Type alias for a valid JSON
   */
  type ValidJson = Validation[String, JValue]

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
