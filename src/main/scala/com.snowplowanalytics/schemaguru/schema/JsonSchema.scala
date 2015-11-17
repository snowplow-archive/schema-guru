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

// This library
import Helpers.SchemaContext
import types.{ZeroSchema, ProductSchema}


/**
 * Base trait for all JSON Schema types
 * Primary methods which every subtype need to implement are:
 * `toJson` represent schema as JSON value
 * `mergeWithSameType` is fine-grained merge with two schemas of the same type
 */
abstract trait JsonSchema {

  /**
   * All Schemas need to have and implicitly pass SchemaContext to it's children
   */
  implicit val schemaContext: SchemaContext

  /**
   * Convert Schema into JSON
   *
   * @return JSON Object with Schema
   */
  def toJson: JObject

  /**
   * Get types of current Schema chunk
   * Types always can be product or absent at all, that's why it's
   * Set[String] and not a String
   *
   * @return set of types for Schema's `type` property
   */
  def getType: Set[String]

  /**
   * Return enum as JValue
   * Enum can be absent, can be sequence or consist of different types in product types
   * To contain enum Schema type need to use ``SchemaWithEnum`` trait
   *
   * @return seq of all possible values for this schema
   */
  def getJEnum: JValue = JNothing

  /**
   * Get partial function merging two same-type schemas with specified enum
   * cardinality
   * e.g. string and string or array and array
   *
   * @return always same type schema
   */
  def mergeSameType(implicit schemaContext: SchemaContext): PartialFunction[JsonSchema, JsonSchema]

  /**
   * Get partial function applying schema with different schema type,
   * creating product type for these two schemas
   * e.g. [string, integer], [object, array], [number, null]
   *
   * @return partial function will return product schema if given argument
   *         has different type
   */
  def createProduct: PartialFunction[JsonSchema, ProductSchema] = {
    case other => ProductSchema()(schemaContext).merge(this).merge(other)
  }

  /**
   * Get partial function merging this schema into product schema
   * e.g. string and [string, number]
   *
   * @return partial function will return product schema if given argument is
   *         product schema too
   */
  def mergeToProduct: PartialFunction[JsonSchema, ProductSchema] = {
    case prod: ProductSchema => prod.merge(this)
  }

  /**
   * Partial function merging this object with zero
   * Kind of identity function
   *
   * @return partial function will always return this schema
   */
  def mergeWithZero: PartialFunction[JsonSchema, JsonSchema] = {
    case ZeroSchema() => this
  }

  /**
   * Primary merge function consequentially applying all auxiliary partial functions:
   * `mergeSameType`, `mergeWithZero`, `mergeToProduct`, `createProduct`,
   * trying to guess how to merge these two schemas
   *
   * @param other schema to merge
   * @return merged schema
   */
  def merge(other: JsonSchema)(implicit schemaContext: SchemaContext): JsonSchema = {
    mergeSameType.orElse(mergeWithZero).orElse(mergeToProduct).orElse(createProduct).apply(other)
  }

  /**
   * Overloaded `merge` working with option
   * Will return current schema if other schema is None
   *
   * @param other optional Schema
   * @return same schema if `other` is None, merged schema otherwise
   */
  def merge(other: Option[JsonSchema])(implicit schemaContext: SchemaContext): JsonSchema = other match {
    case Some(o) => o.merge(this)
    case None    => this
  }

  // Auxiliary functions

  /**
   * Get maximum value of two options. Or None if one of values is None
   *
   * @param first any Option with ordering type
   * @param second any Option with ordering type
   * @return maximum value wrapped in Option
   */
  def maxOrNone[A : Order](first: Option[A], second: Option[A]): Option[A] =
    for { a <- first; b <- second } yield a max b

  /**
   * Get minimum value of two options. Or None if one of values is None
   *
   * @param first any Option with ordering type
   * @param second any Option with ordering type
   * @return minimum value wrapped in Option
   */
  def minOrNone[A : Order](first: Option[A], second: Option[A]): Option[A] =
    for { a <- first; b <- second } yield a min b

  /**
   * Get value wrapped in option if two values are equal, None otherwise
   *
   * @param first any Option with ordering type
   * @param second any Option with ordering type
   * @return minimum value wrapped in Option
   */
  def eqOrNone[A](first: Option[A], second: Option[A]): Option[A] =
    if (first == second) first
    else None
}

