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
import org.json4s._
import org.json4s.JsonDSL._

// This library
import Helpers.SchemaContext

/**
 * Schema for number values
 * http://spacetelescope.github.io/understanding-json-schema/reference/numeric.html#number
 *
 * @param minimum minimum bound
 * @param maximum maximum bound
 * @param enum set of all acceptable values
 */
final case class NumberSchema(
  minimum: Option[Double] = None,
  maximum: Option[Double] = None,
  enum: Option[SortedSet[Double]] = Some(SortedSet.empty[Double])
)(implicit val schemaContext: SchemaContext) extends JsonSchema with SchemaWithEnum[Double] {

  def toJson = {
    val json = ("type" -> "number") ~ ("maximum" -> maximum) ~ ("minimum" -> minimum) ~ ("enum" -> getJEnum) transformField {
      case ("minimum", JDouble(0.0)) => ("minimum" -> 0)
    }
    json.asInstanceOf[JObject]
  }

  def mergeSameType(implicit schemaContext: SchemaContext) = {
    case NumberSchema(min, max, otherEnum) => {
      val mergedEnums = mergeEnums(otherEnum)
      NumberSchema(minOrNone(min, minimum), maxOrNone(max, maximum), mergedEnums)
    }
    case IntegerSchema(min, max, otherEnum) => {
      val mergedEnums = mergeEnums(otherEnum.map(_.map(_.toDouble)))
      NumberSchema(
        (min.map(_.toDouble) |@| minimum) { math.min },
        (max.map(_.toDouble) |@| maximum) { math.max },
        mergedEnums
      )
    }
  }

  def getType = Set("number")

  def jsonConstructor(value: Double) = JDouble(value)
}

