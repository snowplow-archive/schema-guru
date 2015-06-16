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

// Scalaz
import scalaz._
import Scalaz._

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// Argot
import org.clapper.argot._

// This library
import utils.FileSystemJsonGetters

object Main extends App with FileSystemJsonGetters {
  val parser = new ArgotParser(
    programName = "generated.ProjectSettings.name",
    compactUsage = true,
    preUsage = Some("%s: Version %s. Copyright (c) 2015, %s.".format(
      generated.ProjectSettings.name,
      generated.ProjectSettings.version,
      generated.ProjectSettings.organization)
    )
  )

  val directoryArgument = parser.option[ValidJsonList](List("dir"), "directory", "Directory which contains JSONs to be converted") {
    (c, _) => { getJsonsFromFolder(c) }
  }

  val fileArgument = parser.option[Validation[String, JValue]](List("file"), "file", "Single JSON instance to be converted") {
    (c, _) => { getJsonFromFile(c) }
  }

  val outputFileArgument = parser.option[String]("output", "file", "Output file") { (c, _) => c }

  parser.parse(args)

  val jsonList: ValidJsonList = directoryArgument.value match {
    case Some(dir) => dir
    case None      => fileArgument.value match {
      case Some(file) => List(file)
      case None       => parser.usage("either --dir or --file argument must be provided")
    }
  }

  jsonList match {
    case Nil       => parser.usage("Directory does not contain any JSON files.")
    case someJsons => {

      // Upload JsonSchema
      val result = SchemaGuru.convertsJsonsToSchema(someJsons)

      // Print JsonSchema to file or stdout
      outputFileArgument.value match {
        case Some(file) => {
          val output = new java.io.PrintWriter(file)
          output.write(pretty(render(result.schema)))
          output.close()
        }
        case None => println(pretty(render(result.schema)))
      }

      // Print errors
      if (!result.errors.isEmpty) {
        println("Errors:\n " + result.errors.mkString("\n"))
      }

      // Print warnings
      result.warning match {
        case Some(warning) => println(warning.consoleMessage)
        case _ =>
      }
    }
  }
}
