/*
 * Copyright (c) 2014-2015 Snowplow Analytics Ltd. All rights reserved.
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

// Java
import java.io.{ PrintWriter, File }

// Scalaz
import scalaz._
import Scalaz._

/**
 * Utilities for printing and reading to/from files
 */
object FileUtils {
  /**
   * Creates a new file with the contents of the list inside.
   *
   * @param fileName The name of the new file
   * @param fileDir The directory we want the file to live in, w/o trailing slash
   * @param content Content of file
   * @return a success or failure string about the process
   */
  def writeToFile(fileName: String, fileDir: String, content: String): Validation[String, String] = {
    val path = fileDir + "/" + fileName
    try {
      makeDir(fileDir) match {
        case true => {
          // Attempt to open the file...
          val file = new File(path)

          // Print the contents of the list to the new file...
          printToFile(file) { _.println(content) }

          // Output a success message
          s"File [${file.getAbsolutePath}] was written successfully!".success
        }
        case false => s"Could not make new directory to store files in - Check write permissions".failure
      }
    } catch {
      case e: Exception => {
        val exception = e.toString
        s"File [${path}] failed to write: [$exception]".failure
      }
    }
  }

  /**
   * Prints a single line to a file
   *
   * @param f The File we are going to print to
   */
  private def printToFile(f: File)(op: PrintWriter => Unit) {
    val p = new PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }

  /**
   * Creates a new directory at the path
   * specified and returns a boolean on
   * if it was successful.
   *
   * @param dir The path that needs to be
   *            created
   * @return a boolean of direcroty creation
   *         success
   */
  def makeDir(dir: String): Boolean = {
    val file = new File(dir)
    if (!file.exists()) {
      file.mkdirs
    }
    true
  }
}
