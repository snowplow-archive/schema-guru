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
import scalaz._
import Scalaz._

// Scala
import scala.io.{ BufferedSource, Source }

import java.io.File

// Jackson
import com.fasterxml.jackson.core.JsonParseException

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
 * Functions responsible for getting JValues (possible invalid) from files,
 * directories, etc
 */
trait FileSystemJsonGetters {
  /**
   * Recursively get all files in ``dir`` except hidden
   *
   * @param dir directory to scan
   * @return list of found files
   */
  def listAllFiles(dir: File): List[File] = {
    def scanSubdir(subDir: File): Array[File] = {
      val these = subDir.listFiles.filterNot(_.getName.startsWith("."))
      these ++ these.filter(_.isDirectory).flatMap(scanSubdir)
    }
    scanSubdir(dir).filter(_.isFile).toList
  }

  /**
   * Returns a validated List of JSONs from the folder it was pointed at
   *
   * @param dir The directory we are going to get JSONs from
   * @return a List with validated JSONs nested inside
   */
  def getJsonsFromFolder(dir: File): ValidJsonList = {
    val proccessed = for {
      file <- listAllFiles(dir)
    } yield {
        try {
          val content = Source.fromFile(file).mkString
          parse(content).success
        } catch {
          case e: JsonParseException => {
            val exception = e.getMessage
            s"File [${file.getAbsolutePath}}] contents failed to parse into JSON: [$exception]".failure
          }
          case e: Exception => {
            val exception = e.getMessage
            s"File [${file.getAbsolutePath}] fetching and parsing failed: [$exception]".failure
          }
        }
      }
    proccessed.toList
  }

  /**
   * Returns a validated JSON from the specified path
   *
   * @param file file object with JSON
   * @return a validation either be correct JValue or error as String
   */
  def getJsonFromFile(file: File): Validation[String, JValue] = {
    try {
      val content = Source.fromFile(file).mkString
      parse(content).success
    } catch {
      case e: JsonParseException => {
        val exception = e.getMessage
        s"File [${file.getAbsolutePath}] contents failed to parse into JSON: [$exception]".failure
      }
      case e: Exception => {
        val exception = e.getMessage
        s"File [${file.getAbsolutePath}] fetching and parsing failed: [$exception]".failure
      }
    }
  }

  /**
   * Returns a validated List of JSONs from newline-delimited JSON file
   *
   * @param file newline-delimited JSON
   * @return a List with validated JSONs nested inside
   */
  def getJsonFromNDFile(file: File): ValidJsonList = {
    val validatedFile: Validation[String, BufferedSource] = try {
      Source.fromFile(file).success
    } catch {
      case e: Exception => {
        val exception = e.getMessage
        s"File [${file.getAbsolutePath}] fetching and parsing failed: [$exception]".failure
      }
    }

    validatedFile match {
      case Success(content) => {
        val lines = content.mkString.split("\n").zipWithIndex
        val processed =
          for { (json, line) <- lines }
          yield {
            try { parse(json).success }
            catch {
              case e: Exception => {
                val exception = e.getMessage
                s"File [${file.getAbsolutePath}] failed to parse line $line into JSON: [$exception]".failure
              }
            }
          }
        processed.toList
      }
      case Failure(f) => List(f.failure)
    }
  }

  /**
   * Returns a validated List of JSONs from the folder with bunch of new-line
   * delimited JSONS it was pointed at.
   *
   * @param dir The directory we are going to get JSONs from
   * @return a List with validated JSONs nested inside
   */
  def getJsonsFromFolderWithNDFiles(dir: File): ValidJsonList = {
    val proccessed = for {
      file <- listAllFiles(dir)
    } yield {
        getJsonFromNDFile(file)
      }
    proccessed.flatten
  }
}

