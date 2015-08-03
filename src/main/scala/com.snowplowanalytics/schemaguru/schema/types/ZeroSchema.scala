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
import org.json4s._

// This library
import Helpers.SchemaContext

/**
 * Special "phantom" zero element for JsonSchema monoid
 * Represented in JSON as `{}` which will validate ANY JSON
 */
case class ZeroSchema(implicit val schemaContext: SchemaContext) extends JsonSchema {

  // empty object
  def toJson = JObject(Nil)

  def mergeSameType(implicit schemaContext: SchemaContext): PartialFunction[JsonSchema, JsonSchema] = {
    case other => other
  }

  // should never be called
  def getType = Set.empty[String]
}


