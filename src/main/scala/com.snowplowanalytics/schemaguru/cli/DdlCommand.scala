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

// Schema DDL
import com.snowplowanalytics.schemaddl.SchemaData.{ FlatSchema, SelfDescInfo }
import com.snowplowanalytics.schemaddl.generators.SchemaFlattener.{ flattenJsonSchema, getSelfDescElems }
import com.snowplowanalytics.schemaddl.generators.redshift.{
  RedshiftDdlGenerator => RDG,
  Ddl
}
import com.snowplowanalytics.schemaddl.generators.redshift.JsonPathGenerator.getJsonPathsFile
import com.snowplowanalytics.schemaddl.utils.{ StringUtils => SU }

// This library
import utils.FileUtils
import utils.FileSystemJsonGetters.{ splitValidated, getJsonFiles }

/**
 * Class containing all inputs necessary data for `ddl` generation command
 */
case class DdlCommand private[schemaguru](
  input: File,
  output: File,
  db: String = "redshift",  // Isn't checked anywhere
  withJsonPaths: Boolean = false,
  rawMode: Boolean = false,
  schema: Option[String] = None,
  varcharSize: Int = 4096,
  splitProduct: Boolean = false,
  noHeader: Boolean = false) extends SchemaGuruCommand {

  import DdlCommand._

  /**
   * Primary working method of `ddl` command
   * Get all JSONs from specified path, try to parse them as JSON Schemas,
   * convert to `DdlOutput` (combined table definition, JSON Paths, etc)
   * and output them to specified path, also output errors
   */
  def processDdl(): Unit = {
    val allFiles: ValidJsonFileList = getJsonFiles(input)
    val (failures, jsons) = allFiles.foldLeft((List.empty[String], List.empty[JsonFile]))(splitValidated)

    println(failures.mkString("\n"))

    jsons match {
      case Nil       => sys.error(s"Directory [${input.getAbsolutePath}] does not contain any valid JSON files")
      case someJsons =>
        val validatedDdls = someJsons.map(schemaToDdl)
        validatedDdls.map {
          case Success(ddl) => output(ddl)
          case Failure(err) => println(err)
        }
    }
  }

  /**
   * Core function producing JSON Paths file, DDL, warnings and path based on Schema
   *
   * @param json content of JSON file (JSON Schema)
   * @return successful DDL output (table, JSON Paths, warnings) or failure message
   */
  private def schemaToDdl(json: JsonFile): Validation[String, DdlOutput] = {
    for {
      flatSchema <- flattenJsonSchema(json.content, splitProduct)
      ddlFile <- produce(flatSchema, json)
    } yield {
      val jsonPathsLines = if (withJsonPaths) getJsonPathsFile(ddlFile.table.columns, rawMode) else ""
      val header = if (noHeader) "" else ddlFile.header ++ "\n\n"
      val tableDefinition = header ++ ddlFile.schemaCreate ++ "\n\n" ++ ddlFile.snakifyTable.toDdl ++ "\n\n" ++ ddlFile.comment.toDdl
      DdlOutput(jsonPathsLines, tableDefinition, ddlFile.table.warnings, (ddlFile.path, ddlFile.fileName))
    }
  }

  /**
   * Produces all data required for DDL file, including it's path, filename,
   * header and DDL object
   *
   * @param flatSchema fields mapped to it's properties
   * @param json JSON file, containing filename and content
   * @return tuple of values filepath, filename, header and DDL (as object)
   */
  private def produce(flatSchema: FlatSchema, json: JsonFile): Validation[String, DdlFile] = {
    val schemaCreate: String = schema.map(s => Ddl.Schema(s).toDdl).getOrElse("")

    if (rawMode) {
      val fileNameWithoutExtension =
        if (json.fileName.endsWith(".json")) json.fileName.dropRight(5)
        else json.fileName
      val table = RDG.getTableDdl(flatSchema, fileNameWithoutExtension, schema, varcharSize, rawMode)
      val comment = RDG.getTableComment(fileNameWithoutExtension, schema, json.fileName)
      DdlFile(".", fileNameWithoutExtension, RDG.RedshiftDdlHeader, schemaCreate, table, comment).success
    } else {
      getSelfDescElems(json.content).map { self =>
        val tableName = SU.getTableName(self)
        val table = RDG.getTableDdl(flatSchema, tableName, schema, varcharSize, rawMode)
        val combined = getFileName(self)
        val comment = RDG.getTableComment(tableName, schema, self)
        DdlFile(combined._1, combined._2, RDG.RedshiftDdlHeader, schemaCreate, table, comment)
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

    val ddlDir = new File(output, "sql/" + vendor).getAbsolutePath
    FileUtils.writeToFile(file + ".sql", ddlDir, ddlOutput.redshiftDdl).map(println)

    if (withJsonPaths) {
      val jsonPathDir = new File(output, "jsonpaths/" + vendor).getAbsolutePath
      FileUtils.writeToFile(file + ".json", jsonPathDir, ddlOutput.jsonPaths).map(println)
    }
    if (!ddlOutput.warnings.isEmpty) {
      for { warning <- ddlOutput.warnings } println("WARNING: " + warning)
    }
  }
}

/**
 * Companion object containing auxilliary data definitions:
 * DdlFile and DdlOutput
 */
private[schemaguru] object DdlCommand {
  /**
   * Class holding all information for file with DDL
   */
  case class DdlFile(
    path: String,
    fileName: String,
    header: String,
    schemaCreate: String,
    table: Ddl.Table,
    comment: Ddl.Comment
  ) {
    /**
     * Get same DDL file with all column names being snakified
     */
    def snakifyTable: Ddl.Table = {
      val snakified = table.columns.map { column =>
        column.copy(columnName = SU.snakify(column.columnName)) }
      table.copy(columns = snakified)
    }
  }
  
  /**
   * Class holding all information to output
   *
   * @param jsonPaths JSONPaths file content
   * @param redshiftDdl Redshift Table DDL content
   * @param warnings accumulated list of warnings
   * @param filePath tuple of dir and file name
   */
  case class DdlOutput(
    jsonPaths: String,
    redshiftDdl: String,
    warnings: List[String],
    filePath: (String, String)
  )

  /**
   * Get the file path and name from self-describing info
   * Like com.mailchimp/subscribe_1
   *
   * @param flatSelfElems all information from Self-describing schema
   * @return relative filepath
   */
  def getFileName(flatSelfElems: SelfDescInfo): (String, String) = {
    // Make the file name
    val version = "_".concat(flatSelfElems.version.replaceAll("-[0-9]+-[0-9]+", ""))
    val file = flatSelfElems.name.replaceAll("([^A-Z_])([A-Z])", "$1_$2").toLowerCase.concat(version)

    // Return the vendor and the file name together
    (flatSelfElems.vendor, file)
  }
}