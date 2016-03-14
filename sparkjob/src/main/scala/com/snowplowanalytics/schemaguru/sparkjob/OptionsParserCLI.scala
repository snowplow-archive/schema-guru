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

// Scopt
import scopt._

// This library
import Common.SchemaVer

object OptionsParserCLI {
  def parse(args: List[String]): Validation[String, SparkJobCommand] = {
    val parser = getParser
    val parsedCommand = try {
      parser.parse(args, SparkJobCommand("", "")).success
    } catch {
      case e: Exception => e.getMessage.failure
    }
    
    parsedCommand.flatMap {
      case Some(parsed) => parsed.success
      case None => parser.usage.failure
    }
  }

  def getParser: OptionParser[SparkJobCommand] = {
    new OptionParser[SparkJobCommand]("SchemaGuruSparkJob") {
      head(generated.ProjectSettings.name, generated.ProjectSettings.version)
      help("help") text "Print this help message"
      version("version") text "Print version info\n"

      arg[String]("input") action { (x, c) =>
        c.copy(input=x) } required() text "Path to single JSON instance or directory"

      opt[String]("output")
        .action { (x, c) => c.copy(output=x) }
        .required()
        .valueName("<path>")
        .text("Path to output result. File for single Schema, directory (required) for segmentation")

      opt[Int]("enum")
        .action { (x, c) => c.copy(enumCardinality=x) }
        .valueName("<n>")
        .text("Cardinality to evaluate enum property")
        .validate { o => if (o > 0) success else failure("Option --enum cannot be less than zero") }

      opt[Unit]("enum-sets")
        .action { (_, c) => c.copy(enumSets=true) }
        .text("Derive predefined enum sets")

      opt[Unit]("no-length")
        .action { (_, c) => c.copy(noLength=true) }
        .text("Do not try to derive minLength and maxLength for strings\n\tRecommended for small sets")

      opt[Unit]("ndjson")
        .action { (_, c) => c.copy(ndjson=true) }
        .text("Expect newline-delimited JSON")

      opt[String]("vendor")
        .action { (x, c) => c.copy(vendor = Some(x)) }
        .valueName("<title>")
        .text("Vendor title for Self-describing data")
        .validate { o =>
          if (o.matches("([A-Za-z0-9\\-\\_\\.]+)")) success
          else failure("Option --vendor can contain only alphanumeric characters, underscores, hyphens and dots") }

      opt[String]("name")
        .action { (x, c) => c.copy(name=Some(x)) }
        .valueName("<name>")
        .text("Schema name for Self-describing data\n\tCan be omitted with --schema-by")
        .validate { o =>
          if (o.matches("([A-Za-z0-9\\-\\_]+)")) success
          else failure("Option --name can contain only alphanumeric characters, underscores and hyphens") }

      opt[String]("schemaver")
        .action { (x, c) => c.copy(schemaver = SchemaVer.parse(x)) }
        .valueName("<m-r-v>")
        .text("Schema version for Self-describing data in Schemaver format\n\tDefault: 1-0-0")
        .validate { o =>
          if (SchemaVer.parse(o).isDefined) success
          else failure("Option --schemaver must match Schemaver format (example: 1-2-1)") }

      opt[String]("schema-by")
        .action { (x, c) => c.copy(schemaBy=Some(x)) }
        .valueName("<JSON Path>")
        .text("Segment set of instances by JSON Path\n\tValue under JSON Path will be taken as name Self-describing data\n")

      opt[String]("errors-path")
        .action { (x, c) => c.copy(errorsPath = Some(x)) }
        .valueName("path")
        .text("Path to dump errors")

      checkConfig { command =>
        if (command.vendor.isDefined && (command.name.isEmpty && command.schemaBy.isEmpty))
          failure("Option --vendor must be used only in conjunction with --name or --schema-by")
        else if (command.name.isDefined && command.vendor.isEmpty)
          failure("Option --name can be used only in conjunction with --vendor")
        else if (command.schemaBy.isDefined && command.vendor.isEmpty)
          failure("Option --schema-by must be used only in conjunction with --vendor")
        else if (command.schemaver.isDefined && command.vendor.isEmpty && (command.schemaBy.isEmpty || command.name.isEmpty))
          failure("Option --schemaver must be used only in conjunction with --vendor and (--schema-by or --name)")
        else success
      }
    }
  }
}
