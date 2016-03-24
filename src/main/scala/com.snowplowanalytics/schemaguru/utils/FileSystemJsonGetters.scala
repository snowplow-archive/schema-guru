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

// Java
import java.io.File

// Jackson
import com.fasterxml.jackson.core.JsonParseException

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// This library
import Common.{ JsonFile, splitValidations }

/**
 * Functions responsible for getting JValues (possible invalid) from files,
 * directories, etc
 */
object FileSystemJsonGetters {

  /**
   * Inspect what `input` parameter is (file or dir) and load
   * (recursively in case of dir) JSONs as specified format
   * (NDJSON or plain) into list of validations
   *
   * @param input file object referring to file or dir
   * @param ndjson whether files should containing newline-delimilted JSON
   * @return pair with list of error and list of successful parsed JSONs
   */
  def getJsons(input: File, ndjson: Boolean): (List[String], List[JValue]) =
    splitValidations(jsonList(input, ndjson))

  /**
   * Inspect what `input` parameter is (file or dir) and load
   * (recursively in case of dir) JSONs as specified format
   * (NDJSON or plain) into list of validations
   * Note: when trying to load ndjson-file with `ndjson` set to false
   * it will parse only first instance
   * Note: all non-JSON files will be parsed as Failure
   *
   * @param input file object referring to file or dir
   * @param ndjson whether files should containing newline-delimilted JSON
   * @return list of validated JSON instances
   */
  def jsonList(input: File, ndjson: Boolean): ValidJsonList =
    if (input.isDirectory && ndjson)
      getJsonsFromFolderWithNDFiles(input)
    else if (input.isDirectory && !ndjson)
      getJsonsFromFolder(input)
    else if (ndjson && !input.isDirectory)
      getJsonFromNDFile(input)
    else   // single ndjson file
      getJsonFromFile(input) :: Nil

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
  def getJsonsFromFolder(dir: File): ValidJsonList =
    for { file <- listAllFiles(dir) } yield getJsonFromFile(file)

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
   * Get list of JSON Files from either a directory (multiple JSON Files)
   * or single file (one-element list)
   *
   * @param file Java File object pointing to File or dir
   * @return list of validated JsonFiles
   */
  def getJsonFiles(file: File): ValidJsonFileList =
    if (!file.exists()) Failure(s"Path [${file.getAbsolutePath}] doesn't exist") :: Nil
    else if (file.isDirectory) for { f <- listAllFiles(file) } yield getJsonFile(f)
    else getJsonFile(file) :: Nil

  /**
   * Get Json File from a single [[File]] object
   *
   * @param file Java File object
   * @return validated Json File or failure message
   */
  def getJsonFile(file: File): Validation[String, JsonFile] =
    getJsonFromFile(file) match {
      case Success(json) => JsonFile(file.getName, json).success
      case Failure(str) => str.failure
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

  /**
   * Return a validated array or error
   *
   * @param file file object with JSON-array
   * @return a validation either be correct JArray or failed key as a string
   */
  def getJArrayFromFile(file: File): Validation[String, JArray] = {
    val json = for {
      file <- getJsonFromFile(file)
    } yield file

    json match {
      case Success(j: JArray) => j.success
      case _                  => file.getAbsolutePath.failure
    }
  }

  /**
   * Return a validated array or error
   *
   * @param file path to file with JSON-array
   * @return a validation either be correct JArray or failed key as a string
   */
  def getJArrayFromFile(path: String): Validation[String, JArray] =
    getJArrayFromFile(new File(path))
}

