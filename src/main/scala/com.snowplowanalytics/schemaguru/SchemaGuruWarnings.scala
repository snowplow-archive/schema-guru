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

// json4s
import org.json4s._
import org.json4s.JsonDSL._

/**
 * Trait for output a warning messages
 */
// TODO: consider output errors in the same way
trait SchemaWarning {
  /**
   * Output message designed for other app (like Web UI)
   * @return JSON representation of warning
   */
  def jsonMessage: JValue

  /**
   * Output message designed for CLI output
   * @return plain string representation of warning
   */
  def consoleMessage: String
}

object SchemaWarning {
  /**
   * Takes reduced JSON Schema, splits it to auxiliary information and
   * final Schema itself
   *
   * @param reducedSchema schema after ``mergeJsonSchemas``
   * @return pair of final schema and optional aux info
   */
  def splitSchemaAndWarnings(reducedSchema: JValue): (JValue, Option[SchemaWarning]) = {
    implicit val formats = DefaultFormats
    val duplicated = reducedSchema.extractOpt[PossibleDuplicatesWarning]
    val finalSchema = reducedSchema.removeField {
      case ("possibleDuplicates", JArray(_)) => true
      case _ => false
    }
    (finalSchema, duplicated)
  }
}

/**
 * Class container for duplicated pairs
 *
 * @param possibleDuplicates list of two-element lists (pairs)
 *                           containing all possible duplicates
 */
case class PossibleDuplicatesWarning(possibleDuplicates: List[List[String]]) extends SchemaWarning {
  implicit def pairsToJArray(pairs: List[List[String]]): JArray =
    JArray(pairs.map(p => JArray(p.map(s => JString(s)))))

  def isEmpty =
    if (possibleDuplicates.isEmpty || possibleDuplicates.head.isEmpty) true
    else false

  def jsonMessage =
    if (this.isEmpty) { JNothing }
    else { ("message", "Possibly duplicated keys found") ~ ("items", possibleDuplicates) }

  def consoleMessage =
    if (this.isEmpty) { "" }
    else { "Possibly duplicated keys found:\n" + possibleDuplicates.map(_.mkString(": ")).mkString("\n") }
}

