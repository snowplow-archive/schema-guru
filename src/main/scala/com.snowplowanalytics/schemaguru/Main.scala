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

// Argot
import org.clapper.argot._
import org.clapper.argot.ArgotConverters._

// This library
import cli._

object Main extends App {
  private val commands =
    """
      |Currently supported subcommands are:
      |ddl    - use JSON Schema to generate DDL file for specific DB
      |derive - use set of JSON instances to derive JSON Schema
    """.stripMargin

  private val parser = new ArgotParser(
    programName = generated.ProjectSettings.name,
    compactUsage = true,
    preUsage = Some("%s: Version %s. Copyright (c) 2015, %s.".format(
      generated.ProjectSettings.name,
      generated.ProjectSettings.version,
      generated.ProjectSettings.organization)
    )
  )

  val subcommand = parser.parameter[String]("subcommand", "Action to perform", false)
  val helpFlag = parser.flag[Boolean](List("help"), "Output help and exit") // dummy flag, to get around https://github.com/bmc/argot/issues/7

  // Simulate subcommands with argot
  val primaryArgs = args.take(1)    // take only --help or subcommand
  val subcommandArgs = args.drop(1) // hide another options from argot parser

  try {
    parser.parse(primaryArgs)
  } catch {
    case _: ArgotUsageException if helpFlag.value.getOrElse(false) => {
      println(parser.usageString() + commands)
      sys.exit(0)
    }
  }

  subcommand.value match {
    case Some("derive") => DeriveCommand(subcommandArgs)
    case Some("ddl")    => DdlCommand(subcommandArgs)
    case _              => parser.usage("You need to specify subcommand.\n" + commands)
  }
}
