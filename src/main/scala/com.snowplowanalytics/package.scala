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
  case class SchemaGuruResult(schema: JsonSchema, errors: List[String], warning: Option[SchemaWarning] = None)

  /**
   * Class for containing both resulting schema and self-describing info
   *
   * @param schema ready for use JSON Schema object
   * @param selfDescribingInfo optional self-describing information
   */
  case class Schema(schema: JsonSchema, selfDescribingInfo: Option[SelfDescribingSchema]) {

    /**
     * Represent result as JSON
     *
     * @return JSON Object with resulting schema
     */
    def toJson: JObject = selfDescribingInfo match {
      case Some(info) => {
        val uri: JObject = ("$schema", info.selfDescribingSchemaURI)
        val selfObject: JObject = ("self",
          (("vendor", info.vendor): JObject) ~
          (("name", info.name.getOrElse("unspecified")): JObject) ~
          (("version", info.version.getOrElse("1-0-0")): JObject) ~
           ("format", "jsonschema"))

        uri.merge(selfObject).merge(schema.toJson)
      }
      case None       => schema.toJson
    }
  }

  /**
   * Used to annotate schema with data for self-describing schema
   *
   * @param vendor vendor of schema
   * @param name name of schema (parameter isn't optional)
   * @param version optional version
   */
  case class SelfDescribingSchema(vendor: String, name: Option[String], version: Option[String]) {
    val selfDescribingSchemaURI = "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#"
  }

  type ValidJson = Validation[String, JValue]

  type ValidSchema = Validation[String, JsonSchema]

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
