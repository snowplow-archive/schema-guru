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

// Schema DDL
import com.snowplowanalytics.schemaddl.{ FlatSchema, SelfDescInfo }
import com.snowplowanalytics.schemaddl.generators.{
  SchemaFlattener => SF
}
import com.snowplowanalytics.schemaddl.generators.redshift.{
  JsonPathGenerator => JPG,
  RedshiftDdlGenerator => RDG,
  Ddl
}
import com.snowplowanalytics.schemaddl.utils.{ StringUtils => SU }

// This library
import utils.{
  FileUtils => FU,
  FileSystemJsonGetters
}

/**
 * Holds all information passed with CLI and decides how to produce
 * DDL and JSON Paths
 *
 * @param args array of arguments passed via CLI
 */
class DdlCommand(val args: Array[String]) extends FileSystemJsonGetters {
  import DdlCommand._

  // Required
  val inputArgument = parser.parameter[File]("input", "Path to schema or directory with schemas", false)

  // Set all arguments
  val outputOption = parser.option[File](List("output"), "path", "Destination directory")
  val dbOption = parser.option[String](List("db"), "name", "For which DB we need to produce DDL (default: redshift)")
  val withJsonPathsFlag = parser.flag("with-json-paths", false, "Produce JSON Paths files with DDL")
  val rawModeFlag = parser.flag("raw", false, "Produce raw DDL without Snowplow-specific data")
  val schemaOption = parser.option[String](List("schema"), "name", "Redshift schema name")
  val sizeOption = parser.option[Int](List("varchar-size"), "n", "Default size for varchar data type")
  val splitProductFlag = parser.flag("split-product", false, "Split product types into different keys")

  parser.parse(args)

  // Get all arguments
  val input = inputArgument.value.get  // isn't optional
  val outputPath = outputOption.value.getOrElse(new File("."))
  val db = dbOption.value.getOrElse("redshift")
  val withJsonPaths = withJsonPathsFlag.value.getOrElse(false)
  val rawMode = rawModeFlag.value.getOrElse(false)
  val schemaName = if (!rawMode && schemaOption.value.isEmpty) { Some("atomic") } else { schemaOption.value }
  val size = sizeOption.value.getOrElse(4096)
  val splitProduct = splitProductFlag.value.getOrElse(false)

  val schemaList: ValidJsonFileList =
    if (input.isDirectory) {
      getJsonFilesFromFolder(input)
    } else {
      List(getJsonFileFromFile(input))
    }

  (withJsonPaths, splitProduct) match {
    case (true, true) => parser.usage("Options --with-json-paths and --split-product can't be used together")
    case _            =>
  }

  schemaList match {
    case Nil       => parser.usage(s"Directory ${input.getAbsolutePath} does not contain any JSON files")
    case someJsons => someJsons.map(processAndOutput)
  }

  /**
   * Process schema and output JSON Path and DDL
   *
   * @param file file with JSON Schema
   */
  private def processAndOutput(file: Validation[String, JsonFile]): Unit = {
    processSchema(file) match {
      case Success(ddlOutput) => output(ddlOutput)
      case Failure(str) => {
        println(str)
        sys.exit(1)
      }
    }
  }

  /**
   * Produces all data required for DDL file, including it's path, filename,
   * header and DDL object
   *
   * @param flatSchema fields mapped to it's properties
   * @param validJson JSON file, containing filename and content
   * @param rawMode produce Snowplow-specific info
   * @return tuple of values filepath, filename, header and DDL (as object)
   */
  private def getRedshiftDdlFile(flatSchema: FlatSchema, validJson: JsonFile, rawMode: Boolean): Validation[String, DdlFile] = {
    val schemaCreate: String = schemaName.map(s => Ddl.Schema(s).toDdl).getOrElse("")

    if (rawMode) {
      val fileNameWithoutExtension =
        if (validJson.fileName.endsWith(".json")) validJson.fileName.dropRight(5)
        else validJson.fileName
      val table = RDG.getTableDdl(flatSchema, fileNameWithoutExtension, schemaName, size, true)
      val comment = RDG.getTableComment(fileNameWithoutExtension, schemaName, validJson.fileName)
      DdlFile(".", fileNameWithoutExtension, RDG.RedshiftDdlHeader, schemaCreate, table, comment).success
    } else {
      SF.getSelfDescElems(validJson.content).map { self =>
        val tableName = SU.getTableName(self)
        val table = RDG.getTableDdl(flatSchema, tableName, schemaName, size, false)
        val combined = getFileName(self)
        val comment = RDG.getTableComment(tableName, schemaName, self)
        DdlFile(combined._1, combined._2, RDG.RedshiftDdlHeader, schemaCreate, table, comment)
      }
    }
  }

  /**
   * Core function producing JSON Paths file, DDL, warnings and path
   *
   * @param json content of JSON file (JSON Schema)
   * @return all validated information as tuple
   */
  private def processSchema(json: Validation[String, JsonFile]): Validation[String, DdlOutput] = {
    for {
      validJson <- json
      flatSchema <- SF.flattenJsonSchema(validJson.content, splitProduct)
      ddlFile <- getRedshiftDdlFile(flatSchema, validJson, rawMode)
    } yield {
      db match {
        case "redshift" => {
          val jsonPathsLines = if (withJsonPaths) {
            JPG.getJsonPathsFile(ddlFile.table.columns, rawMode)
          } else { "" }

          // Snakify columnNames only after JSONPaths was created
          // TODO: refactor it
          val tableWithSnakedColumns = ddlFile.table.copy(columns = ddlFile.table.columns.map(c => c.copy(columnName = SU.snakify(c.columnName))))

          DdlOutput(
            jsonPathsLines,
            ddlFile.header ++ "\n\n" ++
            ddlFile.schemaCreate ++ "\n\n" ++
            tableWithSnakedColumns.toDdl ++ "\n\n" ++
            ddlFile.comment.toDdl,
            ddlFile.table.warnings,
            (ddlFile.path, ddlFile.fileName)
          )
        }
        case otherDb    => parser.usage(s"Error: DDL generation for $otherDb is not supported yet")
      }
    }
  }

  /**
   * Outputs JSON Path file and DDL file to files in ``destination``
   * or prints errors
   *
   * @param ddlOutput everything we need to output
   */
  private def output(ddlOutput: DdlOutput): Unit = {
    val (vendor, file) = ddlOutput.filePath

    val ddlDir = new File(outputPath, "sql/" + vendor).getAbsolutePath
    FU.writeToFile(file + ".sql", ddlDir, ddlOutput.redshiftDdl).map(println)

    if (withJsonPaths) {
      val jsonPathDir = new File(outputPath, "jsonpaths/" + vendor).getAbsolutePath
      FU.writeToFile(file + ".json", jsonPathDir, ddlOutput.jsonPaths).map(println)
    }
    if (!ddlOutput.warnings.isEmpty) {
      for { warning <- ddlOutput.warnings } println("WARNING: " + warning)
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
   * Class holding all information for file with DDL
   */
  private case class DdlFile(
    path: String,
    fileName: String,
    header: String,
    schemaCreate: String,
    table: Ddl.Table,
    comment: Ddl.Comment
  )

  /**
   * Class holding all information to output
   *
   * @param jsonPaths JSONPaths file content
   * @param redshiftDdl Redshift Table DDL content
   * @param warnings accumulated list of warnings
   * @param filePath tuple of dir and file name
   */
  private case class DdlOutput(jsonPaths: String, redshiftDdl: String, warnings: List[String], filePath: (String, String))

  /**
   * Get the file path and name from self-describing info
   * Like com.mailchimp/subscribe_1
   *
   * @param flatSelfElems all information from Self-describing schema
   * @return relative filepath
   */
  private[cli] def getFileName(flatSelfElems: SelfDescInfo): (String, String) = {
    // Make the file name
    val version = "_".concat(flatSelfElems.version.replaceAll("-[0-9]+-[0-9]+", ""))
    val file = flatSelfElems.name.replaceAll("([^A-Z_])([A-Z])", "$1_$2").toLowerCase.concat(version)

    // Return the vendor and the file name together
    (flatSelfElems.vendor, file)
  }

}
