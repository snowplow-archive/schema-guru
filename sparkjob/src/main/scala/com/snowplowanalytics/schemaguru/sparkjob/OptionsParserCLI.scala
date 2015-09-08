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
package sparkjob

// Scalaz
import scalaz._
import Scalaz._

// Argot
import org.clapper.argot._
import org.clapper.argot.ArgotConverters._

// This library
import generators.PredefinedEnums.predefined

object OptionsParserCLI {
  def parse(args: List[String]): Validation[String, SchemaGuruOptions] =
    try {
      parseArgs(args).success
    } catch {
      case e: ArgotUsageException => e.getMessage.fail
      case e: Exception => e.getMessage.fail
    }

  private def parseArgs(args: List[String]): SchemaGuruOptions = {
    val parser = new ArgotParser(programName = generated.ProjectSettings.name, compactUsage = true)

    val inputArgument = parser.parameter[String]("input", "Path to schema or directory with schemas", false)

    val outputOption = parser.option[String]("output", "path", "Output file (print to stdout otherwise)")
    val cardinalityOption: SingleValueOption[Int] = parser.option[Int](List("enum"), "n", "Cardinality to evaluate enum property")
    val ndjsonFlag = parser.flag[Boolean](List("ndjson"), "Expect ndjson format")
    val schemaByOption = parser.option[String](List("schema-by"), "JSON Path", "Path of Schema title")
    val enumSetsOption = parser.multiOption[String](List("enum-sets"), "set", s"Predefined enum sets (${predefined.keys.mkString(",")})")

    // self-describing schema arguments
    val vendorOption = parser.option[String](List("vendor"), "name", "Vendor name for self-describing schema")
    val nameOption = parser.option[String](List("name"), "name", "Schema name for self-describing schema")
    val versionOption = parser.option[String](List("schemaver"), "version", "Schema version (in SchemaVer format) for self-describing schema")

     // spark-only
    val errorsPathOption = parser.option[String](List("errors-path"), "path", "Path to dump errors")

    parser.parse(args)

    val input = inputArgument.value.get // isn't optional
    outputOption.value match {
      case None    => parser.usage("--output is required for Spark Job")
      case Some(_) =>
    }

    // Get arguments for JSON Path segmentation and validate them
    val segmentSchema: Option[(String, String)] = (schemaByOption.value, outputOption.value) match {
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

    SchemaGuruOptions(
      input,
      outputOption.value,
      cardinalityOption.value.getOrElse(0),
      ndjsonFlag.value.getOrElse(false),
      schemaByOption.value,
      enumSetsOption.value.toList,
      segmentSchema,
      selfDescribing,
      errorsPathOption.value
    )
  }
}

