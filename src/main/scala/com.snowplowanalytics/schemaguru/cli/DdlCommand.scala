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

// Scala
import scala.language.implicitConversions

// Scalaz
import scalaz._
import Scalaz._

// Java
import java.io.File

// Schema DDL
import com.snowplowanalytics.schemaddl.SchemaData.{ FlatSchema, SelfDescInfo }
import com.snowplowanalytics.schemaddl.generators.SchemaFlattener.flattenJsonSchema
import com.snowplowanalytics.schemaddl.generators.redshift.{ RedshiftDdlGenerator => RDG, Ddl }
import com.snowplowanalytics.schemaddl.generators.redshift.JsonPathGenerator.getJsonPathsFile
import com.snowplowanalytics.schemaddl.utils.{ StringUtils => SU }

// This library
import Common._
import utils.FileSystemJsonGetters.getJsonFiles

/**
 * Class containing all inputs necessary data for `ddl` generation command
 */
case class DdlCommand private[cli] (
  input: File,
  output: File,
  db: String = "redshift",        // Isn't checked anywhere
  withJsonPaths: Boolean = false,
  rawMode: Boolean = false,
  schema: Option[String] = None,  // empty for raw, "atomic" for non-raw
  varcharSize: Int = 4096,
  splitProduct: Boolean = false,
  noHeader: Boolean = false,
  force: Boolean = false) extends SchemaGuruCommand {

  import DdlCommand._

  /**
   * Convert schema-ddl Self-describing info to schema-guru self-describing info
   */
  implicit def selfDescSchemas(schema: SchemaDescription): SelfDescInfo =
    SelfDescInfo(schema.vendor, schema.name, schema.version.asString)

  /**
   * Primary working method of `ddl` command
   * Get all JSONs from specified path, try to parse them as JSON Schemas,
   * convert to [[DdlOutput]] (combined table definition, JSON Paths, etc)
   * and output them to specified path, also output errors
   */
  def processDdl(): Unit = {
    val allFiles: ValidJsonFileList = getJsonFiles(input)
    val (failures, jsons) = splitValidations(allFiles)

    if (failures.nonEmpty) {
      println("JSON Parsing errors:")
      println(failures.mkString("\n"))
    }

    jsons match {
      case Nil       => sys.error(s"Directory [${input.getAbsolutePath}] does not contain any valid JSON files")
      case someJsons =>
        val outputs =
          if (rawMode) transformRaw(someJsons)
          else transformSelfDescribing(someJsons)
        outputResult(outputs)
    }
  }

  // Self-describing

  /**
   * Transform list of JSON files to a single [[DdlOutput]] containing
   * all data to produce: DDL files, JSONPath files, migrations, etc
   * 
   * @param files list of valid JSON Files, supposed to be Self-describing JSON Schemas
   * @return transformation result containing all data to output
   */
  private def transformSelfDescribing(files: List[JsonFile]): DdlOutput = {
    val dbSchema = schema.getOrElse("atomic")
    // Parse Self-describing Schemas
    val (schemaErrors, schemas) = splitValidations(files.map(_.extractSelfDescribingSchema))

    // Build table definitions from JSON Schemas
    val validatedDdls = schemas.map(schema => selfDescSchemaToDdl(schema, dbSchema).map(ddl => (schema.self, ddl)))
    val (ddlErrors, ddlPairs) = splitValidations(validatedDdls)
    val ddlMap = groupWithLast(ddlPairs)

    // Build migrations and order-related data
    val migrationMap = Migrations.buildMigrationMap(schemas)
    val validOrderingMap = Migrations.getOrdering(migrationMap)
    val orderingMap = validOrderingMap.collect { case (k, Success(v)) => (k, v) }
    val (migrationErrors, migrations) = splitValidations(Migrations.reifyMigrationMap(migrationMap, Some(dbSchema), varcharSize))

    // Order table-definitions according with migrations
    val ddlFiles = ddlMap.map { case (description, table) =>
      val order = orderingMap.getOrElse(description.revisionCriterion, Nil)
      table.reorderTable(order)
    }.toList
    val ddlWarnings = ddlFiles.flatMap(_.table.warnings)

    // Build DDL-files and JSONPaths file (in correct order and camelCased column names)
    val outputPair = for {
      ddl <- ddlFiles
    } yield (makeDdlFile(ddl), makeJsonPaths(ddl))

    DdlOutput(
      outputPair.map(_._1),
      migrations,
      outputPair.flatMap(_._2),
      warnings = schemaErrors ++ ddlErrors ++ ddlWarnings)
  }

  /**
   * Transform valid Self-describing JSON Schema to DDL table definition
   *
   * @param schema valid JSON Schema including all Self-describing information
   * @param dbSchema DB schema name ("atomic")
   * @return validation of either table definition or error message
   */
  private def selfDescSchemaToDdl(schema: Schema, dbSchema: String): Validation[String, TableDefinition] = {
    val ddl = for {
      flatSchema <- flattenJsonSchema(schema.data, splitProduct)
    } yield produceTable(flatSchema, schema.self, dbSchema)
    ddl match {
      case Failure(fail) => (fail + s" in [${schema.self.toPath}] Schema").failure
      case success => success
    }
  }

  /**
   * Produce table from flattened Schema and valid JSON Schema description
   *
   * @param flatSchema ordered map of flatten JSON properties
   * @param description JSON Schema description
   * @param dbSchema DB schema name ("atomic")
   * @return table definition
   */
  private def produceTable(flatSchema: FlatSchema, description: SchemaDescription, dbSchema: String): TableDefinition = {
    val schemaCreate = Ddl.Schema(dbSchema).toDdl
    val combined     = getFileName(description)
    val tableName    = SU.getTableName(description)
    val table        = RDG.getTableDdl(flatSchema, tableName, Some(dbSchema), varcharSize, rawMode)
    val comment      = RDG.getTableComment(tableName, Some(dbSchema), description)
    TableDefinition(combined._1, combined._2, RDG.RedshiftDdlHeader, schemaCreate, table, comment)
  }


  // Raw

  /**
   * Transform list of raw JSON Schemas (without Self-describing info)
   * to raw Ddl output (without migrations, additional data and
   * without explicit order)
   *
   * @param files list of JSON Files (assuming JSON Schemas)
   * @return transformation result containing all data to output
   */
  private def transformRaw(files: List[JsonFile]): DdlOutput = {
    val (schemaErrors, ddlFiles) = splitValidations(files.map(jsonToRawTable))
    val ddlWarnings = ddlFiles.flatMap(_.table.warnings)

    val outputPair = for {
      ddl <- ddlFiles
    } yield (makeDdlFile(ddl), makeJsonPaths(ddl))

    DdlOutput(outputPair.map(_._1), Nil, outputPair.flatMap(_._2), warnings = schemaErrors ++ ddlWarnings)
  }

  /**
   * Generate table definition from raw (non-self-describing JSON Schema)
   *
   * @param json JSON Schema
   * @return validated table definition object
   */
  private def jsonToRawTable(json: JsonFile): Validation[String, TableDefinition] = {
    val ddl = flattenJsonSchema(json.content, splitProduct).map { flatSchema =>
      produceRawTable(flatSchema, json.fileName)
    }
    ddl match {
      case Failure(fail) => (fail + s" in [${json.fileName}] file").failure
      case success => success
    }
  }

  /**
   * Produces all data required for raw DDL file, including it's path, filename,
   * header and DDL object. Raw file however doesn't contain anything
   * Snowplow- or Iglu-specific, thus JsonFile isn't required to be Self-describing
   * and we cannot produce migrations or correct column order for raw DDL
   *
   * @param flatSchema fields mapped to it's properties
   * @param fileName JSON file, containing filename and content
   * @return DDL File object with all required information to output it
   */
  private def produceRawTable(flatSchema: FlatSchema, fileName: String): TableDefinition = {
    val name         = if (fileName.endsWith(".json")) fileName.dropRight(5) else fileName
    val schemaCreate = schema.map(Ddl.Schema(_).toDdl).getOrElse("")
    val table        = RDG.getTableDdl(flatSchema, name, schema, varcharSize, rawMode)
    val comment      = RDG.getTableComment(name, schema, fileName)
    TableDefinition(".", name, RDG.RedshiftDdlHeader, schemaCreate, table, comment)
  }


  // Common

  /**
   * Make Redshift DDL file out of table definition
   *
   * @param ddl table definition object
   * @return text file with Redshift table DDL
   */
  private def makeDdlFile(ddl: TableDefinition): TextFile = {
    val header = if (noHeader) "" else ddl.header ++ "\n\n"
    val schemaCreate = if (ddl.schemaCreate.isEmpty) "" else ddl.schemaCreate ++ "\n\n"
    val tableDefinition = header ++ schemaCreate ++ ddl.snakifyTable.toDdl ++ "\n\n" ++ ddl.comment.toDdl
    TextFile(new File(new File(ddl.path), ddl.fileName + ".sql"), tableDefinition)
  }

  /**
   * Make JSONPath file out of table definition
   *
   * @param ddl table definition
   * @return text file with JSON Paths if option is set
   */
  private def makeJsonPaths(ddl: TableDefinition): Option[TextFile] = {
    val jsonPath = withJsonPaths.option(getJsonPathsFile(ddl.table.columns, rawMode))
    jsonPath.map { content =>
      TextFile(new File(new File(ddl.path), ddl.fileName + ".json"), content)
    }
  }

  /**
   * Output end result
   */
  def outputResult(result: DdlOutput): Unit = {
    result.ddls
          .map(_.setBasePath("sql"))
          .map(_.setBasePath(output.getAbsolutePath))
          .map(_.write(force)).map(printMessage)

    result.jsonPaths
          .map(_.setBasePath("jsonpaths"))
          .map(_.setBasePath(output.getAbsolutePath))
          .map(_.write(force)).map(printMessage)

    result.migrations
          .map(_.setBasePath("sql"))
          .map(_.setBasePath(output.getAbsolutePath))
          .map(_.write(force)).map(printMessage)

    result.warnings.map(printMessage)
  }
}

/**
 * Companion object containing auxilliary data definitions:
 * DdlFile and DdlOutput
 */
private[cli] object DdlCommand {

  /**
   * Class holding an aggregated output ready to be written
   * with all warnings collected due transofmrations
   *
   * @param ddls list of files with table definitions
   * @param migrations list of files with available migrations
   * @param jsonPaths list of JSONPaths files
   * @param warnings all warnings collected in process of parsing and
   *                 transformation
   */
  case class DdlOutput(
    ddls: List[TextFile],
    migrations: List[TextFile] = Nil,
    jsonPaths: List[TextFile] = Nil,
    warnings: List[String] = Nil)

  /**
   * Class holding all information for file with DDL
   *
   * @param path base directory for file
   * @param fileName DDL file name
   * @param header comment with warnings
   * @param schemaCreate DB schema CREATE statement
   * @param table table object
   * @param comment table comment object
   */
  case class TableDefinition(
    path: String,
    fileName: String,
    header: String,
    schemaCreate: String,
    table: Ddl.Table,
    comment: Ddl.Comment) {
    /**
     * Get same DDL file with all column names being snakified
     */
    def snakifyTable: Ddl.Table = {
      val snakified = table.columns.map { column =>
        column.copy(columnName = SU.snakify(column.columnName)) }
      table.copy(columns = snakified)
    }

    /**
     * Pick columns listed in `order` (and presented in [[table]]
     * at the same time) and appends them to the end of original [[table]],
     * leaving not listed in `order` on their places
     *
     * @param order sublist of column names in right order that should be
     *              appended to the end of table
     * @return DDL file object with reordered columns
     */
    def reorderTable(order: List[String]): TableDefinition = {
      val columns = table.columns
      val columnMap = columns.map(c => (c.columnName, c)).toMap
      val addedOrderedColumns = order.flatMap(columnMap.get(_))
      val columnsToSort = order intersect columns.map(_.columnName)
      val initialColumns = columns.filterNot(c => columnsToSort.contains(c.columnName))
      val orderedColumns = initialColumns ++ addedOrderedColumns
      this.copy(table = this.table.copy(columns = orderedColumns))
    }
  }

  /**
   * Get the file path and name from self-describing info
   * Like com.mailchimp/subscribe_1
   *
   * @param flatSelfElems all information from Self-describing schema
   * @return relative filepath
   */
  def getFileName(flatSelfElems: SchemaDescription): (String, String) = {
    // Make the file name
    val version = "_".concat(flatSelfElems.version.asString.replaceAll("-[0-9]+-[0-9]+", ""))
    val file = flatSelfElems.name.replaceAll("([^A-Z_])([A-Z])", "$1_$2").toLowerCase.concat(version)

    // Return the vendor and the file name together
    (flatSelfElems.vendor, file)
  }

  /**
   * Print value extracted from scalaz Validation or any other message
   */
  private def printMessage(any: Any): Unit = {
    any match {
      case Success(m) => println(m)
      case Failure(m) => println(m)
      case m => println(m)
    }
  }

  /**
   * Aggregate list of Description-Definition pairs into map, so in value will be left
   * only [[TableDefinition]] for latest revision-addition Schema
   * Use this to be sure vendor_tablename_1 always generated for latest Schema
   *
   * @param ddls list of pairs
   * @return Map with latest table definition for each Schema addition
   */
  private def groupWithLast(ddls: List[(SchemaDescription, TableDefinition)]) = {
    val aggregated = ddls.foldLeft(Map.empty[ModelCriterion, (SchemaDescription, TableDefinition)]) {
      case (acc, (description, definition)) =>
        acc.get(description.modelCriterion) match {
          case Some((desc, defn)) if desc.version.revision < description.version.revision =>
            acc ++ Map((description.modelCriterion, (description, definition)))
          case Some((desc, defn)) if desc.version.revision == description.version.revision &&
            desc.version.addition < description.version.addition =>
            acc ++ Map((description.modelCriterion, (description, definition)))
          case None =>
            acc ++ Map((description.modelCriterion, (description, definition)))
          case _ => acc
        }
    }
    aggregated.map { case (revision, (desc, defn)) => (desc, defn) }
  }
}
