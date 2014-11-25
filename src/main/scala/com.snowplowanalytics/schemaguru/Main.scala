/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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

// Generators
import generators.{
  JsonSchemaGenerator => JSG,
  JsonSchemaMerger => JSM
}

// Scalaz
import scalaz._
import Scalaz._

// Jackson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.core.JsonParseException

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.scalaz.JsonScalaz._

/**
 * Launches the process to convert JSONs into JSONSchema
 */
object Main {

  // TODO: remove folder hardcoding
  private val PathToRoot = "//vagrant//schema-guru//src//main//resources//"
  // System.getProperty("user.dir")

  def main(args: Array[String]) {

    // We can ask for the Folder Path in the CLI Args
    // val dirPath = args(0) - Will need to check that this exists before trying to grab it.

    // RUNTIME OPERATIONS //

    val returned = getJsonFromFolder(PathToRoot).toList

    val successes = for {
      Success(json) <- returned
    } yield {
      JSG.jsonToSchema(json)
    }

    println(pretty(render(successes(0))))

    val merged = JSM.mergeJsonSchemas(successes)

    println(pretty(render(merged)))
  }

  /**
   * Returns a validated List of JSONs from the folder it was
   * pointed at.
   *
   * @param dir The directory we are going to get JSONs from
   * @param ext The extension of the file we are going to be
   *        attempting to grab
   * @return an Array with validated JSONs nested inside
   */
  private def getJsonFromFolder(dir: String, ext: String = "txt"): Array[Validation[String, JValue]] =
    for {
      filePath <- new java.io.File(dir).listFiles.filter(_.getName.endsWith("." + ext))
    } yield {
      try {
        val file = scala.io.Source.fromFile(filePath)
        val content = file.mkString
        parse(content).success
      } catch {
        case e: JsonParseException => {
          val exception = e.toString
          s"File [$filePath] contents failed to parse into JSON: [$exception]".fail
        }
        case e: Exception => {
          val exception = e.toString
          s"File [$filePath] fetching and parsing failed: [$exception]".fail
        }
      }
    }
}
