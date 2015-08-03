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

// json4s
import org.json4s.JsonDSL._
import org.json4s._

// This library
import Helpers.SchemaContext

/**
 * Schema for array values
 * http://spacetelescope.github.io/understanding-json-schema/reference/array.html
 *
 * @param items child schema. Currently doesn't support tuple validation
 */
final case class ArraySchema(items: JsonSchema)(implicit val schemaContext: SchemaContext) extends JsonSchema with SchemaWithTransform[ArraySchema] {

  def toJson = ("type" -> "array") ~ ("items" -> items.toJson)

  def mergeSameType(implicit schemaContext: SchemaContext) = {
    case ArraySchema(its) => ArraySchema(its.merge(items))
  }

  def getType = Set("array")

  def transform(f: PartialFunction[JsonSchema, JsonSchema]): ArraySchema = {
    val its = items match {
      case complex: SchemaWithTransform[_] => complex.transform(f)
      case primitive                       => if (f.isDefinedAt(primitive)) f(primitive) else primitive
    }
    this.copy(its)
  }
}
