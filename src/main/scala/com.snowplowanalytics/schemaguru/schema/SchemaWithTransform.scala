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

/**
 * Trait for all complex schema types: arrays, objects, products
 *
 * @tparam S recursive type for self-reference
 */
trait SchemaWithTransform[S <: SchemaWithTransform[S]] extends JsonSchema { self: S =>
  /**
   * Accept partial function operating on primitive types and recursively apply
   * it to all nested subschemas
   *
   * @param f partial function operating on primitive subschemas
   * @return complex schema of the same type
   */
  def transform(f: PartialFunction[JsonSchema, JsonSchema]): SchemaWithTransform[S]
}
