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
package cli

import scalaz._
import Scalaz._

// Java
import java.io.File

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// Argot
import org.clapper.argot._
import org.clapper.argot.ArgotConverters._

// This library
import generators.PredefinedEnums.predefined
import schema.Helpers.SchemaContext
import utils._

/**
 * Holds all information passed with CLI and decides how to produce
 * JSON Schema
 *
 * @param args array of arguments passed via CLI
 */
class SchemaCommand(val args: Array[String]) extends FileSystemJsonGetters {
  import SchemaCommand._

  // Required
  val inputArgument = parser.parameter[File]("input", "Path to schema or directory with schemas", false)

  // primary subcommand's options and arguments
  val outputOption = parser.option[String]("output", "path", "Output file (print to stdout otherwise)")
  val cardinalityOption = parser.option[Int](List("enum"), "n", "Cardinality to evaluate enum property")
  val ndjsonFlag = parser.flag[Boolean](List("ndjson"), "Expect ndjson format")
  val schemaByOption = parser.option[String](List("schema-by"), "JSON Path", "Path of Schema title")
  val enumSets = parser.multiOption[String](List("enum-sets"), "set", s"Predefined enum sets (${predefined.keys.mkString(",")})")

  // self-describing schema arguments
  val vendorOption = parser.option[String](List("vendor"), "name", "Vendor name for self-describing schema")
  val nameOption = parser.option[String](List("name"), "name", "Schema name for self-describing schema")
  val versionOption = parser.option[String](List("schemaver"), "version", "Schema version (in SchemaVer format) for self-describing schema")

  parser.parse(args)

  val input = inputArgument.value.get // isn't optional

  // Get arguments for JSON Path segmentation and validate them
  val segmentSchema = (schemaByOption.value, outputOption.value) match {
    case (Some(jsonPath), Some(dirPath)) => Some((jsonPath, dirPath))
    case (Some(jsonPath), None)          => Some((jsonPath, "."))
    case _                               => None
  }

  // Get arguments for self-describing schema and validate them
  val selfDescribing = (vendorOption.value, nameOption.value, versionOption.value) match {
    case (Some(vendor), name, version) => {
      name match {
        case None if (!segmentSchema.isDefined)   => parser.usage("You need to specify --name OR segment schema.")
        case Some(_) if (segmentSchema.isDefined) => parser.usage("You need to specify --name OR segment schema.")
        case _ => ()    // we can omit name, but it must be
      }
      if (!vendor.matches("([A-Za-z0-9\\-\\_\\.]+)")) {
        parser.usage("--vendor argument must consist of only letters, numbers, hyphens, underscores and dots")
      } else if (name.isDefined && !name.get.matches("([A-Za-z0-9\\-\\_]+)")) {
        parser.usage("--name argument must consist of only letters, numbers, hyphens and underscores")
      } else if (version.isDefined && !version.get.matches("\\d+\\-\\d+\\-\\d+")) {
        parser.usage("--schemaver argument must be in SchemaVer format (example: 1-1-0)")
      }
      Some(SelfDescribingSchema(vendor, name, version))
    }
    case (None, None, None) => None
    case _  => parser.usage("--vendor, --name and --schemaver arguments need to be used in conjunction.")
  }

  val enumCardinality = cardinalityOption.value.getOrElse(0)
  val schemaContext = SchemaContext(enumCardinality, getEnumSets.flatMap { case Success(l) => List(l); case Failure(_) => Nil })

  // Decide where and which files should be parsed
  val jsonList: ValidJsonList =
    if (input.isDirectory) ndjsonFlag.value match {
      case Some(true) => getJsonsFromFolderWithNDFiles(input)
      case _          => getJsonsFromFolder(input)
    }
    else ndjsonFlag.value match {
      case Some(true) => getJsonFromNDFile(input)
      case _          => List(getJsonFromFile(input))
    }

  jsonList match {
    case Nil => parser.usage("Directory does not contain any JSON files.")
    case someJsons => {
      segmentSchema match {
        case None => {
          val convertResult = SchemaGuru.convertsJsonsToSchema(someJsons, schemaContext)
          val result = SchemaGuru.mergeAndTransform(convertResult, schemaContext)
          outputResult(result, outputOption.value, selfDescribing)
        }
        case Some((path, dir)) => {
          val nameToJsonsMapping = JsonPathExtractor.mapByPath(path, jsonList)
          nameToJsonsMapping map {
            case (key, jsons) => {
              val convertResult = SchemaGuru.convertsJsonsToSchema(jsons, schemaContext)
              val result = SchemaGuru.mergeAndTransform(convertResult, schemaContext)
              val describingInfo = selfDescribing.map(_.copy(name = Some(key)))
              val fileName = key + ".json"
              val file =
                if (key == "$SchemaGuruFailed") None
                else Some(new File(dir, fileName).getAbsolutePath)
              outputResult(result, file, describingInfo)
            }
          }
        }
      }
    }

      /**
       * Print Schema, warnings and errors
       *
       * @param result Schema Guru result containing all information
       * @param outputFile optional path to file for schema output
       * @param selfDescribingInfo optional info to make shema self-describing
       */
      def outputResult(result: SchemaGuruResult, outputFile: Option[String], selfDescribingInfo: Option[SelfDescribingSchema]): Unit = {
        // Make schema self-describing if necessary
        val schema = Schema(result.schema, selfDescribingInfo)

        val errors = result.errors ++ getEnumSets.flatMap {
          case Failure(f) => List(s"Predefined enum [$f] not found")
          case Success(_) => Nil
        }

        // Print JsonSchema to file or stdout
        outputFile match {
          case Some(file) => {
            val output = new java.io.PrintWriter(file)
            output.write(pretty(render(schema.toJson)))
            output.close()
          }
          case None => println(pretty(render(schema.toJson)))
        }

        // Print errors
        if (!errors.isEmpty) {
          println("\nErrors:\n" + errors.mkString("\n"))
        }

        // Print warnings
        result.warning match {
          case Some(warning) => println(warning.consoleMessage)
          case _ =>
        }
      }
  }

  /**
   * Get validated list of enums predefined in
   * `generators.PredefinedEnums.predefined` and in filesystem
   *
   * @return validated list predefined enums
   */
  def getEnumSets: List[Validation[String, JArray]] = {
    val fileArrays: List[Validation[String, JArray]] =
      enumSets.value.toList.filterNot(_ == "all").map(arg => getJArrayFromFile(new File(arg)))

    if (enumSets.value.contains("all")) { predefined.values.map(_.success).toList ++ fileArrays }
    else fileArrays.map {
      case Success(arr) => arr.success
      case Failure(key) if predefined.isDefinedAt(key) => predefined(key).success
      case Failure(arg) => arg.failure
    }
  }
}

/**
 * Companion object holding all static information about command
 */
object SchemaCommand extends GuruCommand {
  val title = "schema"
  val description = "Derive JSON Schema from set of JSON instances"
  val parser = new ArgotParser(programName = generated.ProjectSettings.name + " " + title, compactUsage = true)

  def apply(args: Array[String]) = new SchemaCommand(args)
}
