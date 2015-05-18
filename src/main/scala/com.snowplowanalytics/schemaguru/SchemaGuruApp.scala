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

// Scala
import scala.collection.JavaConversions._

// Jackson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.core.JsonParseException

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.scalaz.JsonScalaz._

// Argot
import org.clapper.argot._

// This library
import generators.{
  JsonSchemaGenerator => JSG,
  JsonSchemaMerger => JSM
}

/**
 * An app to convert JSON events into valid JsonSchemas
 */
object SchemaGuruApp extends App {

  import SchemaGuru._

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
    (c, _) => { SchemaGuru.getJsonsFromFolder(c) }
  }

  val outputFileArgument = parser.option[String]("output", "file", "Output file") { (c, _) => c }

  parser.parse(args)

  val jsonList = directoryArgument
    .value
    .getOrElse(parser.usage("--dir argument must be provided"))

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

      // Print Errors
      if (!result.errors.isEmpty) {
        println("Errors:\n " + result.errors.mkString("\n"))
      }
    }
  }
}

/**
 * Companion object for the
 * SchemaGuruApp
 */
object SchemaGuru {

  /**
   * Takes the valid list of JSONs
   * and returns the JsonSchema
   *
   * @param list The Validated JSON list
   * @return the final JsonSchema
   */
  // TODO: Turn JsonSchemaGenerator into Akka Actor
  def convertsJsonsToSchema(list: ValidJsonList): SchemaGuruResult = {

    // TODO: Throw error if goodJsons list is Nil
    val goodJsons = for {
      Success(json) <- list
    } yield {
      JSG.jsonToSchema(json)
    }

    val badJsons = for {
      Failure(err) <- list
    } yield err

    SchemaGuruResult(JSM.mergeJsonSchemas(goodJsons), badJsons.toList)
  }

  /**
   * Returns a validated List of JSONs from the folder it was
   * pointed at.
   *
   * @param dir The directory we are going to get JSONs from
   * @param ext The extension of the file we are going to be
   *        attempting to grab
   * @return a List with validated JSONs nested inside
   */
  def getJsonsFromFolder(dir: String, ext: String = "json"): ValidJsonList = {
    val proccessed = for {
      filePath <- new java.io.File(dir).listFiles.filter(_.getName.endsWith("." + ext))
    } yield {
      try {
        val file = scala.io.Source.fromFile(filePath)
        val content = file.mkString
        parse(content).success
      } catch {
        case e: JsonParseException => {
          val exception = e.getMessage
          s"File [$filePath] contents failed to parse into JSON: [$exception]".fail
        }
        case e: Exception => {
          val exception = e.getMessage
          s"File [$filePath] fetching and parsing failed: [$exception]".fail
        }
      }
    }
    proccessed.toList
  }
}
