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
package cli

// scalaz
import scalaz._
import Scalaz._

// Java
import java.io.File
import java.io.PrintWriter

// json4s
import org.json4s.{ JArray, JValue }
import org.json4s.jackson.JsonMethods.pretty

// This library
import Common.{ SchemaGuruResult, SchemaVer }
import generators.PredefinedEnums.predefined
import schema.Helpers.SchemaContext
import utils.JsonPathExtractor
import utils.FileSystemJsonGetters.{ getJsons, getJArrayFromFile }

/**
 * Class containing all inputs necessary data for `schema` derivation command
 * and some generated preferences related to `schema` command
 */
case class SchemaCommand private[schemaguru](
  input: File,
  output: Option[File] = None,
  enumCardinality: Int = 0,
  enumSets: Seq[String] = Nil,
  vendor: Option[String] = None,
  name: Option[String] = None,
  schemaver: Option[SchemaVer] = None,
  schemaBy: Option[String] = None,
  noLength: Boolean = false,
  ndjson: Boolean = false) extends SchemaGuruCommand {

  /**
   * Preference representing Schema-segmentation
   * First element of pair is JSON Path, by which we split our Schemas
   * Second element is output path where to put Schemas (can't print it to stdout)
   * None means we don't need to do segmentation
   */
  val segmentSchema = schemaBy.map { jsonPath =>
    (jsonPath, output.getOrElse(new File(".")))
  }

  /**
   * List (probably empty) of JSON Arrays with requested enum sets
   */
  lazy val successfulEnumSets = validatedEnumSets.toList.flatten

  /**
   * Validated list of enums predefined as JSON arrays in
   * `generators.PredefinedEnums.predefined` and in filesystem
   * Custom enums (from filesystem) have priority over predefined
   */
  lazy val validatedEnumSets: ValidationNel[String, List[JArray]] = {
    val (predefinedSetsKeys, customSetsKeys) = enumSets.toList
      .partition(s => s == "all" || predefined.isDefinedAt(s))

    val predefinedSets =
      if (predefinedSetsKeys.contains("all")) predefined.values.toList
      else predefinedSetsKeys.collect(predefined)

    val customSets = customSetsKeys
      .map(getJArrayFromFile(_).leftMap(set => s"Enum set [$set] does not exist").toValidationNel)
      .sequenceU

    customSets.map { _ ++ predefinedSets }
  }

  /**
   * Primary working method of `schema` command
   * Get all JSONs from specified path, parse them,
   * produce single Schema or bunch of segmented Schemas
   * and output them to specified path, also output errors
   */
  def processSchema(): Unit = {
    val (failures, jsons) = getJsons(input, ndjson)

    jsons match {
      case Nil => sys.error(s"Directory [${input.getAbsolutePath}] does not contain any JSON files")
      case someJsons => segmentSchema match {
        case Some((path, dir)) => processSegmented(someJsons, path, dir, failures)
        case None =>
          val result = produce(name, someJsons).appendErrors(failures)
          output(result, output)
      }
    }
  }

  /**
   * Primary method for Schema-segmentation process
   * Segment Schemas by JSON Path and output each Schema to
   * specified output (`dir` (required) + name found in JSON Path)
   * along with Schema-specific errors and all other errors (parsing)
   *
   * @param jsons list of successfully parsed JSON instances
   * @param jsonPath JSON Path presented in `jsons` by instances should be distincted
   *                 and where name of Schema is storing
   * @param dir path to output all segmented Schemas
   * @param parseErrors errors (non-fatal) risen on previous steps
   */
  private def processSegmented(jsons: List[JValue], jsonPath: String, dir: File, parseErrors: List[String]): Unit = {
    val (failures, segmentedJsons) = JsonPathExtractor.segmentByPath(jsonPath, jsons)
    segmentedJsons.foreach { case (key, someJsons) =>
      val result = produce(key.some, someJsons).appendErrors(failures)
      val file = new File(dir, key + ".json").some
      output(result, file)
    }

    // Print errors
    if (parseErrors.nonEmpty) {
      println("\nErrors:\n" + parseErrors.mkString("\n"))
    }
  }

  /**
   * Run process of Schema derivation, mapping each of `jsons` to its microschema,
   * reducing (extending, actually) these microschema to a single Schema
   * validating all these instances, transform this Schema's fields to meaningful
   * values. In the end, optionally describe Schema with name, vendor
   * if user specified ones
   *
   * @param name optional name to describe Schema (non-optional for segmentation)
   * @param jsons list of instances to generate Schema from
   * @return generation result, containing Schema, all warnings and errors happened
   *         during process of derivation
   */
  private def produce(name: Option[String], jsons: List[JValue]): SchemaGuruResult = {
    val context = SchemaContext(enumCardinality, successfulEnumSets, Some(jsons.length), !noLength)
    val convertResult = SchemaGuru.convertsJsonsToSchema(jsons, context)
    SchemaGuru.mergeAndTransform(convertResult, context)
      .describe(vendor, name, schemaver)
  }

  /**
   * Print Schema, warnings and errors
   *
   * @param result Schema Guru result containing all information
   * @param outputFile optional file for schema output, print to stdout if None
   */
  private def output(result: SchemaGuruResult, outputFile: Option[File]): Unit = {
    val json = pretty(result.schema.toJson)

    // Print JsonSchema to file or stdout
    outputFile match {
      case Some(file) =>
        val output = new PrintWriter(file)
        output.write(json)
        output.close()
      case None => println(json)
    }

    // Print errors
    if (result.errors.nonEmpty) {
      println("\nErrors:\n" + result.errors.mkString("\n"))
    }

    // Print warnings
    result.warning match {
      case Some(warning) => println(warning.consoleMessage)
      case _ =>
    }
  }
}
