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
package types

// Scalaz
import scalaz._
import Scalaz._

// Scala
import scala.collection.immutable.SortedSet

// json4s
import org.json4s.JsonDSL._
import org.json4s._

// This library
import Helpers.SchemaContext

/**
 * Schema for integer values
 * http://spacetelescope.github.io/understanding-json-schema/reference/numeric.html#integer
 *
 * @param minimum minimum bound
 * @param maximum maximum bound
 * @param enum set of all acceptable values
 */
final case class IntegerSchema(
  minimum: Option[BigInt] = None,
  maximum: Option[BigInt] = None,
  enum: Option[SortedSet[BigInt]] = Some(SortedSet.empty[BigInt])
)(implicit val schemaContext: SchemaContext) extends JsonSchema with SchemaWithEnum[BigInt] {

  def toJson = ("type" -> "integer") ~ ("maximum" -> maximum) ~ ("minimum" -> minimum) ~ ("enum" -> getJEnum)

  def mergeSameType(implicit schemaContext: SchemaContext) = {
    case IntegerSchema(min, max, otherEnum) => {
      val mergedEnums = mergeEnums(otherEnum)
      IntegerSchema(minOrNone(min, minimum), maxOrNone(max, maximum), mergedEnums)
    }
    case num: NumberSchema => num.merge(this)
  }

  def getType = Set("integer")

  def jsonConstructor(value: BigInt) = JInt(value)
}

