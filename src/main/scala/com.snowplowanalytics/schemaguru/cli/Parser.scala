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

// Scalaz
import scalaz. { Success, Failure }

// Java
import java.io.File

// scopt
import scopt.OptionParser

// This library
import generators.PredefinedEnums.predefined
import Common.SchemaVer

object Parser {
  val parser = new OptionParser[CommandContainer]("schema-guru") {
    head(generated.ProjectSettings.name, generated.ProjectSettings.version)
    help("help") text "Print this help message"
    version("version") text "Print version info\n"

    cmd("schema")
      .action { (_, c) => c.copy(command = Some(SchemaCommand(input = new File("")))) }
      .text("Derive JSON Schema out of JSON instances\n")
      .children (

        arg[File]("input")
          action { (x, c) => c.copy(command = c.setInput(x)) }
          required()
          text "Path to single JSON instance or directory",

        opt[File]("output")
          action { (x, c) => c.setOutput(x) }
          valueName "<path>"
          text "Path to output result. File for single Schema, directory (required) for segmentation\n\tDefault: print to stdout (single) or to current dir (segmentation)",

        opt[Int]("enum")
          action { (x, c) => c.copy(command = c.setEnumCardinality(x)) }
          valueName "<n>"
          text "Cardinality to evaluate enum property. 0 means do not derive\n\tDefault: 0"
          validate { o => if (o > 0) success else failure("Option --enum cannot be less than zero") },

        opt[Seq[String]]("enum-sets")
          action { (x, c) => c.copy(command = c.setEnumSets(x)) }
          valueName "[<set1>,<set2>,<file1>...|all]"
          text s"Predefined enum sets. JSON files containing arrays can be specified\n\tAvailable sets: ${predefined.keys.mkString(", ")}",

        opt[Unit]("no-length")
          action { (_, c) => c.copy(command = c.setNoLength(true)) }
          text "Do not try to derive minLength and maxLength for strings\n\tRecommended for small sets",

        opt[Unit]("ndjson")    action { (_, c) =>
          c.copy(command = c.setNdjson(true)) }
          text "Expect newline-delimited JSON",

        opt[Unit]("no-additional-properties")    action { (_, c) =>
          c.copy(command = c.setNoAdditionalProperties(true)) }
          text "Switch additionalProperties to false\n\tAdditional properties denied",

        opt[String]("vendor")
          action { (x, c) => c.copy(command = c.setVendor(x)) }
          valueName "<title>"
          text "Vendor title for Self-describing data"
          validate { o =>
            if (o.matches("([A-Za-z0-9\\-\\_\\.]+)")) success
            else failure("Option --vendor can contain only alphanumeric characters, underscores, hyphens and dots") },

        opt[String]("name")
          action { (x, c) => c.copy(command = c.setName(x)) }
          valueName "<name>"
          text "Schema name for Self-describing data\n\tCan be omitted with --schema-by"
          validate { o =>
            if (o.matches("([A-Za-z0-9\\-\\_]+)")) success
            else failure("Option --name can contain only alphanumeric characters, underscores and hyphens") },

        opt[String]("schemaver")
          action { (x, c) =>
            SchemaVer.parse(x) match {
              case Some(ver) => c.copy(command = c.setSchemaver(ver))
              case None => c
            }
          }
          valueName "<m-r-v>"
          text "Schema version for Self-describing data in Schemaver format\n\tDefault: 1-0-0"
          validate { o =>
            if (SchemaVer.parse(o).isDefined) success
            else failure("Option --schemaver must match Schemaver format (example: 1-2-1)") },

        opt[String]("schema-by")
          action { (x, c) => c.copy(command = c.setSchemaBy(x)) }
          valueName "<JSON Path>"
          text "Segment set of instances by JSON Path\n\tValue under JSON Path will be taken as name Self-describing data\n",

        checkConfig { // this can be simplified
          case CommandContainer(Some(command: SchemaCommand))
            if !command.input.exists() =>
            failure(s"Input [${command.input.getAbsolutePath}] does not exists")
          case CommandContainer(Some(command: SchemaCommand))
            if command.vendor.isDefined && (command.name.isEmpty && command.schemaBy.isEmpty) =>
            failure("Option --vendor must be used only in conjunction with --name or --schema-by")
          case CommandContainer(Some(command: SchemaCommand))
            if command.name.isDefined && command.vendor.isEmpty =>
            failure("Option --name can be used only in conjunction with --vendor")
          case CommandContainer(Some(command: SchemaCommand))
            if command.schemaBy.isDefined && command.vendor.isEmpty =>
            failure("Option --schema-by must be used only in conjunction with --vendor")
          case CommandContainer(Some(command: SchemaCommand))
            if command.schemaver.isDefined && command.vendor.isEmpty && (command.schemaBy.isEmpty || command.name.isEmpty) =>
            failure("Option --schemaver must be used only in conjunction with --vendor and (--schema-by or --name)")
          case CommandContainer(Some(command: SchemaCommand)) => command.validatedEnumSets match {
            case Success(_) => success
            case Failure(f) => failure(f.list.mkString("\n"))
          }
          case _ => success
        }
      )

    cmd("ddl")
      .action { (_, c) => c.copy(command = Some(DdlCommand(input = new File(""), output = new File(".")))) }
      .text("Generate DDL out of JSON Schema\n")
      .children(

        arg[File]("input")
          action { (x, c) => c.copy(command = c.setInput(x)) } required()
          text "Path to single JSON schema or directory with JSON Schemas",

        opt[File]("output")
          action { (x, c) => c.setOutput(x) }
          valueName "<path>"
          text "Directory to put generated data\n\tDefault: current dir",

        opt[String]("schema")
          action { (x, c) => c.copy(command = c.setSchema(x)) }
          valueName "<name>"
          text "Redshift schema name\n\tDefault: atomic",

        opt[String]("db")
          action { (x, c) => c.copy(command = c.setDb(x)) }
          valueName "<name>"
          text "DB to which we need to generate DDL\n\tDefault: redshift",

        opt[Int]("varchar-size")
          action { (x, c) => c.copy(command = c.setVarcharSize(x)) }
          valueName "<n>"
          text "Default size for varchar data type\n\tDefault: 4096",

        opt[Unit]("with-json-paths")
          action { (_, c) => c.copy(command = c.setWithJsonPaths(true)) }
          text "Produce JSON Paths files with DDL",

        opt[Unit]("raw-mode")
          action { (_, c) => c.copy(command = c.setRawMode(true)) }
          text "Produce raw DDL without Snowplow-specific data",

        opt[Unit]("split-product")
          action { (_, c) => c.copy(command = c.setSplitProduct(true)) }
          text "Split product types (like [string,integer]) into separate columns",

        opt[Unit]("no-header")
          action { (_, c) => c.copy(command = c.setNoHeader(true)) }
          text "Do not place header comments into output DDL",

        opt[Unit]("force")
          action { (_, c) => c.copy(command = c.setForce(true)) }
          text "Force override existing manually-edited files",

        checkConfig {
          case CommandContainer(Some(command: DdlCommand))
            if !command.input.exists() =>
            failure(s"Input [${command.input.getAbsolutePath}] does not exists")
          case CommandContainer(Some(command: DdlCommand))
            if command.withJsonPaths && command.splitProduct =>
            failure("Options --with-json-paths and --split-product cannot be used together")
          case _ => success
        }
      )
  }
}
