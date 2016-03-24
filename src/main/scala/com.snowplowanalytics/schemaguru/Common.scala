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

// Scalaz
import scalaz._
import Scalaz._

// File
import java.io.File

// json4s
import org.json4s._
import org.json4s.JsonDSL._

// This library
import schema.JsonSchema
import utils.FileUtils.writeToFile

/**
 * Module containing common data types used in Schema Guru
 */
object Common {

  lazy implicit val formats = SchemaCodecs.formats

  /**
   * Helper function for [[splitValidations]]
   */
  def splitValidation[F, S](acc: (List[F], List[S]), current: Validation[F, S]): (List[F], List[S]) =
    current match {
      case Success(json) => (acc._1, json :: acc._2)
      case Failure(fail) => (fail :: acc._1, acc._2)
    }

  /**
   * Split list of scalaz Validation in pair of List with successful values
   * and List with unsuccessfule values
   *
   * @param validations probably empty list of Scalaz Validations
   * @tparam F failure type
   * @tparam S success type
   * @return tuple with list of failure and list of successes
   */
  def splitValidations[F, S](validations: List[Validation[F, S]]): (List[F], List[S]) =
    validations.foldLeft((List.empty[F], List.empty[S]))(splitValidation)


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
  case class SchemaGuruResult(schema: DerivedSchema, errors: List[String], warning: Option[SchemaWarning] = None) {

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
    def describe(vendor: Option[String], name: Option[String], version: Option[SchemaVer]): SchemaGuruResult = {
      val description = (vendor |@| name) { (v, n) =>
        SchemaDescription(v, n, version.getOrElse(SchemaVer(1,0,0)))
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
  case class DerivedSchema(schema: JsonSchema, description: Option[SchemaDescription]) {

    /**
     * Represent result as JSON
     *
     * @return JSON Object with resulting schema
     */
    def toJson: JObject = description match {
      case Some(info) =>
        val uri: JObject     = "$schema" -> SchemaDescription.uri
        val vendor: JObject  = "vendor"  -> info.vendor
        val name: JObject    = "name"    -> info.name
        val version: JObject = "version" -> info.version.asString
        val format: JObject  = "format"  -> "jsonschema"
        val selfObject: JObject =
          "self" -> vendor ~ name ~ version ~ format

        uri.merge(selfObject).merge(schema.toJson)
      case None => schema.toJson
    }
  }

  /**
   * Class representing JSON file with path and parsed content
   */
  case class JsonFile(fileName: String, content: JValue) {
    /**
     * Try to extract Self-describing JSON Schema from JSON file
     * [[JsonFile]] not neccessary contains JSON Schema, it also used for storing
     * plain JSON, so this method isn't always successful
     *
     * @return validation JSON Schema
     */
    def extractSelfDescribingSchema: Validation[String, Schema] = {
      val optionalSchema = content.extractOpt[Schema].map(_.success[String])
      optionalSchema.getOrElse(s"Cannot extract Self-describing JSON Schema from JSON file [$fileName]".failure)
    }
  }

  /**
   * Class representing file path and content ready
   * to be written to file system
   *
   * @param file java file object for future write
   * @param content text (DDL) to write
   */
  case class TextFile(file: File, content: String) {
    /**
     * Prepend directory path to Migration file
     *
     * @param dir single directory or composed path (OS-compat on user behalf)
     * @return modified (not-mutated) with new base path
     */
    def setBasePath(dir: String): TextFile =
      this.copy(file = new File(new File(dir), file.getPath))

    /**
     * Try to write [[content]] to [[file]] on file system
     *
     * @return validation with success or error message
     */
    def write: Validation[String, String] =
      writeToFile(file.getName, file.getParentFile.getAbsolutePath, content)
  }

  /**
   * Companion object for [[TextFile]] with additional constructors
   */
  object TextFile {
    def apply(file: String, content: String): TextFile =
      TextFile(new File(file), content)
  }

  // Iglu-core

  /**
   * Class representing cannonical SchemaVer (semantical versioning for Schemas)
   * http://snowplowanalytics.com/blog/2014/05/13/introducing-schemaver-for-semantic-versioning-of-schemas/
   */
  case class SchemaVer(model: Int, revision: Int, addition: Int) {
    def asString = s"$model-$revision-$addition"
  }

  /**
   * Compantion object for [[SchemaVer]]
   */
  object SchemaVer {
    val modelRevisionAdditionRegex = "([0-9]+)-([0-9]+)-([0-9]+)".r

    /**
     * Extract the model, revision, and addition of the SchemaVer
     *
     * @return some SchemaVer or None
     */
    def parse(version: String): Option[SchemaVer] = version match {
      case modelRevisionAdditionRegex(m, r, a) => Some(SchemaVer(m.toInt, r.toInt, a.toInt))
      case _ => None }
  }

  /**
   * Self-description for Schema
   */
  case class SchemaDescription(vendor: String, name: String, version: SchemaVer) {
    /**
     * Get Schema Criterion for this revison
     * Example: com.acme/someEvent/1-2-* for com.acme/someEvent/1-2-2
     */
    def revisionCriterion: RevisionCriterion =
      (vendor, name, version.model, version.revision)

    /**
     * Get Schema Criterion for this model
     * Example: com.acme/someEvent/1-*-* for com.acme/someEvent/1-2-2
     */
    def modelCriterion: ModelCriterion =
      (vendor, name, version.model)

    /**
     * Get cannonical Schema path
     *
     * @return
     */
    def toPath: String =
      s"$vendor/$name/jsonschema/${version.asString}"
  }

  /**
   * Companion object for [[SchemaDescription]]
   */
  object SchemaDescription {
    val uri = "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#"
  }

  /**
   * Object representing self-describing schema with its data
   *
   * @param self schema-description
   * @param data schema itself
   */
  case class Schema(self: SchemaDescription, data: JObject) {
    def revisionCriterion: RevisionCriterion = self.revisionCriterion
  }
}
