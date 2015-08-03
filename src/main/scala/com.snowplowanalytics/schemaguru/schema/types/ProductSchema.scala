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
import org.json4s._
import org.json4s.JsonDSL._

// This library
import Helpers.SchemaContext

/**
 * Product Schema is any schema that contain more than one type
 * Each contained type is represented by one field.
 * e.g. ["string", "number"], ["integer", "null"], ["object", "null"] etc
 *
 * @param objectSchema type information related to "object" type
 * @param arraySchema type information related to "array" type
 * @param stringSchema type information related to "string" type
 * @param integerSchema type information related to "integer" type
 * @param numberSchema type information related to "number" type
 * @param booleanSchema type information related to "boolean" type
 * @param nullSchema type information related to null
 */
final case class ProductSchema (
  objectSchema: Option[ObjectSchema] = None,
  arraySchema: Option[ArraySchema] = None,
  stringSchema: Option[StringSchema] = None,
  integerSchema: Option[IntegerSchema] = None,
  numberSchema: Option[NumberSchema] = None,
  booleanSchema: Option[BooleanSchema] = None,
  nullSchema: Option[NullSchema] = None
)(implicit val schemaContext: SchemaContext) extends JsonSchema with SchemaWithTransform[ProductSchema] {

  /**
   * List of all subtypes that this product schema really contains
   *
   * @return list of types
   */
  def types: List[JsonSchema] =
    List(objectSchema, arraySchema, stringSchema, integerSchema, numberSchema, booleanSchema, nullSchema).flatten

  def toJson =
    types
      .map(_.toJson)
      .foldLeft(JObject(Nil))(_.merge(_))  // this merge can break associativity
      // everything afterwards overrides previous values
      .merge(("type" -> getType): JObject)
      .transformField { case ("enum", _) => ("enum" -> getJEnum) }
      .asInstanceOf[JObject]

  def mergeSameType(implicit schemaContext: SchemaContext): PartialFunction[JsonSchema, ProductSchema] = {
    case ProductSchema(obj, arr, str, int, num, bool, nul) => ProductSchema(
      mergeWithOption(obj, this.objectSchema).asInstanceOf[Option[ObjectSchema]],
      mergeWithOption(arr, this.arraySchema).asInstanceOf[Option[ArraySchema]],
      mergeWithOption(str, this.stringSchema).asInstanceOf[Option[StringSchema]],
      mergeInteger(int),
      mergeInteger(num, int),
      mergeWithOption(bool,this.booleanSchema).asInstanceOf[Option[BooleanSchema]],
      mergeWithOption(nul, this.nullSchema).asInstanceOf[Option[NullSchema]]
    )
  }

  override def merge(other: JsonSchema)(implicit schemaContext: SchemaContext): ProductSchema = other match {
    case prod: ProductSchema =>
      this.mergeSameType(schemaContext)(other)
    case obj: ObjectSchema =>
      this.copy(objectSchema = obj.merge(this.objectSchema).asInstanceOf[ObjectSchema].some)
    case arr: ArraySchema =>
      this.copy(arraySchema = arr.merge(this.arraySchema).asInstanceOf[ArraySchema].some)
    case str: StringSchema =>
      this.copy(stringSchema = str.merge(this.stringSchema).asInstanceOf[StringSchema].some)
    case int: IntegerSchema =>
      if (this.numberSchema.isDefined) // merge int to numberSchema's place and erase current integerSchema
        this.copy(numberSchema = int.merge(this.numberSchema).asInstanceOf[NumberSchema].some, integerSchema = None)
      else                             // merge int as usual
        this.copy(integerSchema = int.merge(this.integerSchema).asInstanceOf[IntegerSchema].some)
    case num: NumberSchema =>          // number and integer can't co-exist in same product type
      this.copy(integerSchema = None, numberSchema = num.merge(this.numberSchema).merge(this.integerSchema).asInstanceOf[NumberSchema].some)
    case bool: BooleanSchema =>
      this.copy(booleanSchema = bool.merge(this.booleanSchema).asInstanceOf[BooleanSchema].some)
    case nul: NullSchema =>
      this.copy(nullSchema = nul.merge(this.nullSchema).asInstanceOf[NullSchema].some)
    case zer: ZeroSchema =>
      this
  }

  def getType = types.map(_.getType).flatten.toSet

  def transform(f: PartialFunction[JsonSchema, JsonSchema]): ProductSchema =
    this.copy(
      objectSchema.map(_.transform(f)),
      arraySchema.map(_.transform(f)),
      stringSchema.map(s => if (f.isDefinedAt(s)) f(s) else s).asInstanceOf[Option[StringSchema]],
      integerSchema.map(i => if (f.isDefinedAt(i)) f(i) else i).asInstanceOf[Option[IntegerSchema]],
      numberSchema.map(n => if (f.isDefinedAt(n)) f(n) else n).asInstanceOf[Option[NumberSchema]]
    )

  override def getJEnum = types.map(_.getJEnum).reduceOption((a, b) => b.merge(a))

  /**
   * Merge two optional schemas or return exist schema if another is None
   *
   * @param a first schema type
   * @param b second schema type
   * @return merged if both present, none if both not present
   */
  def mergeWithOption(a: Option[JsonSchema], b: Option[JsonSchema]) = (a, b) match {
    case (Some(s), Some(o)) => Some(o.mergeSameType(schemaContext)(s))
    case (None, None)       => None
    case (Some(s), None)    => Some(s)
    case (None, Some(o))    => Some(o)
  }

  /**
   * Logic of merging integers and numbers in Product types
   * This function (and it's overloaded companion) will decide what schema type
   * should be returned on merging two product types
   *
   * @param int other's ``ProductType`` integer schema
   * @return
   */
  private def mergeInteger(int: Option[IntegerSchema]): Option[IntegerSchema] = {
    if (this.numberSchema.isDefined) { None }
    else { mergeWithOption(int, this.integerSchema).asInstanceOf[Option[IntegerSchema]] }
  }

  /**
   * Logic of merging integers and numbers in Product types
   * This function (and it's overloaded companion) will decide what schema type
   * should be returned on merging two product types
   *
   * @param num other's ``ProductType`` number schema
   * @param int other's ``ProductType`` integer schema
   * @return
   */
  private def mergeInteger(num: Option[NumberSchema], int: Option[IntegerSchema]): Option[NumberSchema] = {
    if (this.numberSchema.isDefined && num.isDefined) {
      val intMerged = mergeWithOption(int, this.numberSchema).asInstanceOf[Option[NumberSchema]]
      mergeWithOption(num, intMerged).asInstanceOf[Option[NumberSchema]]
    }
    else { mergeWithOption(num, this.numberSchema).asInstanceOf[Option[NumberSchema]] }
  }
}
