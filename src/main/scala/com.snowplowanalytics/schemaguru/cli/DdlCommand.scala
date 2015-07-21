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

// Scalaz
import scalaz._
import Scalaz._

// Java
import java.io.File

// Argot
import org.clapper.argot._
import ArgotConverters._

// Igluutils
import com.snowplowanalytics.igluutils._
import com.snowplowanalytics.igluutils.generators.{
  JsonPathGenerator => JPG,
  SchemaFlattener => SF
}
import com.snowplowanalytics.igluutils.generators.redshift.{ RedshiftDdlGenerator => RDG }
import com.snowplowanalytics.igluutils.utils.{ FileUtils => FU }

/**
 * Holds all information passed with CLI and decides how to produce
 * DDL and JSON Paths
 *
 * @param args array of arguments passed via CLI
 */
class DdlCommand(val args: Array[String]) {
  import DdlCommand._

  // Required
  val inputArgument = parser.parameter[File]("input", "Path to schema or directory with schemas", false)

  // Set all arguments
  val outputOption = parser.option[File](List("output"), "path", "Destination directory")
  val dbOption = parser.option[String](List("db"), "name", "For which DB we need to produce DDL (default: redshift)")
  val withJsonPathsFlag = parser.flag("with-json-paths", false, "Produce JSON Paths files with DDL")
  val rawModeFlag = parser.flag("raw", false, "Produce raw DDL without Snowplow-specific data")
  val schemaOption = parser.option[String](List("schema"), "name", "Redshift schema name")
  val sizeOption = parser.option[Int](List("size"), "n", "Default size for varchar data type")
  val splitProductFlag = parser.flag("split-product", false, "Split product types into different keys")

  parser.parse(args)

  // Get all arguments
  val input = inputArgument.value.get  // isn't optional
  val outputPath = outputOption.value.getOrElse(new File("."))
  val db = dbOption.value.getOrElse("redshift")
  val withJsonPaths = withJsonPathsFlag.value.getOrElse(false)
  val rawMode = rawModeFlag.value.getOrElse(false)
  val schemaName = schemaOption.value
  val size = sizeOption.value.getOrElse(255)
  val splitProduct = splitProductFlag.value.getOrElse(false)

  // Check how to handle path
  if (input.isDirectory) {
    fetchAndParseFromDirectory(input)
  } else {
    fetchAndParseFromFile(input)
  }

  /**
   * Get all files from specified ``dir`` and tries to fetch, process and
   * output JSON Path and DDL from all found files
   *
   * @param dir directory with JSON Schemas
   */
  private def fetchAndParseFromDirectory(dir: File): Unit = {
    val schemas = FU.listSchemas(dir)
    schemas.map(fetchAndParseFromFile(_))
  }

  /**
   * Fetch JSON Schema from specified ``file``, process and output JSON Path
   * and DDL
   *
   * @param file file with JSON Schema
   */
  private def fetchAndParseFromFile(file: File): Unit = {
    processFile(file) match {
      case Success((jsonPathLines, redshiftLines, warningLines, combined)) =>
        output(jsonPathLines, redshiftLines, warningLines, combined)
      case Failure(str) => {
        println(s"Error in [${file.getAbsolutePath}]")
        println(str)
        sys.exit(1)
      }
    }
  }

  /**
   * Core function producing JSON Paths file, DDL, warnings and path
   *
   * @param file JSON Schema file
   * @return all validated information as tuple
   */
  def processFile(file: File): Validation[String, (List[String], List[String], List[String], (String, String))] = {
    for {
      json <- FU.getJsonFromFile(file)
      flatSchema <- SF.flattenJsonSchema(json, splitProduct)
    } yield {
      val combined = getFileName(flatSchema.self)

      val ddl = db match {
        case "redshift" => RDG.getRedshiftDdl(flatSchema, schemaName, size, rawMode)
        case otherDb => parser.usage(s"Error: DDL generation for $otherDb is not supported yet")
      }
      val jsonPathLines = JPG.getJsonPathsFile(flatSchema)

      (jsonPathLines, ddl.content.split("\n").toList, ddl.warnings, combined)
    }
  }

  /**
   * Outputs JSON Path file and DDL file to files in ``destination``
   * or prints errors
   *
   * @param jpf list of JSON Paths
   * @param rdf Validated list of DDL lines
   * @param combined vendor and filename
   */
  private def output(jpf: List[String], rdf: List[String], warnings: List[String], combined: (String, String)): Unit = {
    val (vendor, file) = combined

    val ddlDir = new File(outputPath, "sql/" + vendor).getAbsolutePath
    FU.writeListToFile(file + ".sql", ddlDir, rdf).map(println)

    if (withJsonPaths) {
      val jsonPathDir = new File(outputPath, "jsonpaths/" + vendor).getAbsolutePath
      FU.writeListToFile(file + ".json", jsonPathDir, jpf).map(println)
    }
    if (!warnings.isEmpty) {
      for { warning <- warnings } println("WARNING: " + warning)
    }
  }
}

/**
 * Companion object holding all static information about command
 */
object DdlCommand extends GuruCommand {
  val title = "ddl"
  val description = "Derive DDL using JSON Schema"
  val parser = new ArgotParser(programName = generated.ProjectSettings.name + " " + title, compactUsage = true)

  def apply(args: Array[String]) = new DdlCommand(args)

  /**
   * Get the file path and name from self-describing info
   * Like com.mailchimp/subscribe_1
   *
   * @param flatSelfElems all information from Self-describing schema
   * @return relative filepath
   */
  private def getFileName(flatSelfElems: SelfDescInfo): (String, String) = {
    // Make the file name
    val file = flatSelfElems.name.replaceAll("([^A-Z_])([A-Z])", "$1_$2").toLowerCase.concat("_1")

    // Return the vendor and the file name together
    (flatSelfElems.vendor, file)
  }

}
