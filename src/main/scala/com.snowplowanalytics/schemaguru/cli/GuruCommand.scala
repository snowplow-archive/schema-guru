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

// Java
import java.io.File

// Argot
import org.clapper.argot._

trait GuruCommand {
  // Helper method
  def apply(args: Array[String])

  // Subcommand itself
  val title: String

  // Description for --help
  val description: String

  // Every subcommand has it's own parser
  val parser: ArgotParser

  /**
   * Function to implicitly convert string with path argument to File
   *
   * @param path valid path to file
   * @param opt command-line argument
   * @return Java's File if it exists
   */
  implicit def convertFilePath(path: String, opt: CommandLineArgument[File]): File = {
    val file = new File(path)
    if (!file.exists) {
      parser.usage(s"Input file [$path] does not exist.")
    }
    file
  }

}
