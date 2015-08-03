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

// Scala
import collection.immutable.SortedSet

// json4s
import org.json4s._
import org.json4s.JsonDSL._

// This library
import Helpers.SchemaContext

/**
 * Trait for Schemas which can have enums
 *
 * @tparam A Scala type for enums
 */
abstract trait SchemaWithEnum[A] extends JsonSchema {
  /**
   * Optional set of enum values
   * None will eliminate all following enum merges
   */
  val enum: Option[SortedSet[A]]

  /**
   * How to wrap enum values in JValue
   * e.g. JString(_) for strings
   *
   * @param value value of tparam ``A``
   * @return constructed JSON
   */
  def jsonConstructor(value: A): JValue

  /**
   * Render enum values to JArray or JNothing
   */
  override def getJEnum: JValue = {
    enum.map { set =>
      if (set.size <= schemaContext.enumCardinality) {
        JArray(set.map(jsonConstructor(_)).toList)
      } else {
        JNothing
      }
    }
  }

  /**
   * Merge enums from two schemas. If one of enums is None result will be None
   *
   * @param otherEnum set of enum values
   * @param enumCardinality cardinality of result set.
   *                        If result will exceed limit it will be eliminated to None
   * @return merged set of enum values
   */
  def mergeEnums(otherEnum: Option[SortedSet[A]])(implicit enumCardinality: SchemaContext): Option[SortedSet[A]] = {
    (enum |@| otherEnum) { _ ++ _ } flatMap { e => if (e.size > enumCardinality.enumCardinality) None else Some(e) }
  }
}

