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

// json4s
import org.json4s.JsonDSL._

// This library
import Helpers._

/**
 * Schema for object values
 * http://spacetelescope.github.io/understanding-json-schema/reference/object.html
 *
 * @param properties map of keys to subschemas
 */
final case class ObjectSchema(properties: Map[String, JsonSchema])(implicit val schemaContext: SchemaContext) extends JsonSchema with SchemaWithTransform[ObjectSchema] {

  def toJson = ("type" -> "object") ~ ("properties" -> properties.map {
    case (key, value) => key -> value.toJson
  }) ~ ("additionalProperties" -> false)

  def mergeSameType(implicit schemaContext: SchemaContext) = {

    // Get monoid
    implicit val monoid = getMonoid(schemaContext.enumCardinality)

    // Return partial function
    { case ObjectSchema(props) => ObjectSchema(properties |+| props) }
  }

  def getType = Set("object")

  def transform(f: PartialFunction[JsonSchema, JsonSchema]): ObjectSchema = {
    val props = properties.map {
      case (k, v) => v match {
        case complex: SchemaWithTransform[_] => (k, complex.transform(f))
        case primitive                       => (k, if (f.isDefinedAt(primitive)) f(primitive) else primitive)
      }
    }
    this.copy(properties = props)
  }
}

