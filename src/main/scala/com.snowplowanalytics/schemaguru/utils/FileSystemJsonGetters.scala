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
package utils

// Scalaz
import scalaz.Scalaz._
import scalaz._

// Scala
import scala.io.Source

// Jackson
import com.fasterxml.jackson.core.JsonParseException

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 * Functions responsible for getting JValues (possible invalid)
 * from files, directories, etc
 */
trait FileSystemJsonGetters {
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
          val file = Source.fromFile(filePath)
          val content = file.mkString
          parse(content).success
        } catch {
          case e: JsonParseException => {
            val exception = e.getMessage
            s"File [$filePath] contents failed to parse into JSON: [$exception]".failure
          }
          case e: Exception => {
            val exception = e.getMessage
            s"File [$filePath] fetching and parsing failed: [$exception]".failure
          }
        }
      }
    proccessed.toList
  }

  /**
   * Returns a validated JSON from the specified path
   *
   * @param filePath path to JSON
   * @return a validation either be correct JValue or error as String
   */
  def getJsonFromFile(filePath: String): Validation[String, JValue] = {
    try {
      val content = Source.fromFile(filePath).mkString
      parse(content).success
    } catch {
      case e: JsonParseException => {
        val exception = e.getMessage
        s"File [$filePath] contents failed to parse into JSON: [$exception]".failure
      }
      case e: Exception => {
        val exception = e.getMessage
        s"File [$filePath] fetching and parsing failed: [$exception]".failure
      }
    }
  }
}

