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
  // List of all supported commands
  private val commandsList = List(DdlCommand, SchemaCommand)  // companion objects with static info
  private val commandsMap: Map[String, GuruCommand] = (for { c <- commandsList } yield (c.title, c)).toMap

  // Help message
  private val subcommandsHelp = "Subcommands are:\n" + commandsMap.map {
    case (title, command) => title + " - " + command.description
  }.mkString("\n")

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
  val subcommandArgs = args.drop(1) // subcommand arguments
  try {
    parser.parse(primaryArgs)
  } catch {
    case _: ArgotUsageException if helpFlag.value.getOrElse(false) => {
      println(parser.usageString() + "\n" + subcommandsHelp)
      sys.exit(0)
    }
  }

  // Find command in commandsMap and execute it with args
  subcommand.value.flatMap(commandsMap.get(_)) match {
    case Some(command) => command(subcommandArgs)
    case _             => parser.usage("You need to specify subcommand.\n" + subcommandsHelp)
  }
}