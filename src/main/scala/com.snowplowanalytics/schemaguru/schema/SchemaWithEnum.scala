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
package schema

// Scalaz
import scalaz._
import Scalaz._

// json4s
import org.json4s._
import org.json4s.JsonDSL._

// This library
import Helpers.SchemaContext

/**
 * Trait for Schemas which can have enums
 */
abstract trait SchemaWithEnum extends JsonSchema {
  /**
   * Optional set of enum values
   * None will eliminate all following enum merges
   */
  val enum: Option[List[JValue]]

  /**
   * Render `this` enum values to JArray or JNothing
   */
  override def getJEnum: JValue = enum.map(transformToJArray)

  /**
   * Render some enum values to JArray or JNothing
   */
  private def transformToJArray(enum: List[JValue]): JArray =
    JArray(enum)

  /**
   * Merge enums from two schemas. If one of enums is None or size of merged
   * exceeds enum cardinality result will be None
   *
   * @param otherEnum set of enum values
   * @param schemaContext cardinality of result set.
   *                        If result will exceed limit it will be eliminated to None
   * @return merged set of enum values
   */
  def mergeEnums(otherEnum: Option[List[JValue]])(implicit schemaContext: SchemaContext): Option[List[JValue]] = {
    (enum |@| otherEnum) { _ ++ _ } flatMap { merged =>
      if (merged.size <= schemaContext.enumCardinality) {
        Some(merged.distinct)
        // TODO: check performance on large sets; isPredefinedEnum is very wasteful on every merge
        //       and with constructEnum and substituteEnums(schemaContext) it is unnecessary
        //       also we can maintain keys of matched predefined sets to reduce unnecessary checks
      } else if (schemaContext.isPredefinedEnum(transformToJArray(merged))) {
        Some(merged)
      } else {
        None
      }
    }
  }
}

