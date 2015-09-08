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
import types._

/**
 * Helper and transform functions
 */
object Helpers extends Serializable {

  /**
   * Context used to create and merge schemas and contain all auxiliary
   * information like enum cardinality, special validation requirements and
   * everything related that user can pass from UI.
   * Usually context created based on specific requirements
   * and passing around implicitly everywhere create and merge happening
   *
   * @param enumCardinality maximum limit of enum values
   * @param enumSets list of all predefined enums
   */
  case class SchemaContext(enumCardinality: Int, enumSets: List[JArray] = Nil) {
    private lazy val sets: List[(Set[JValue], Int)] = enumSets.map { e =>
      val set = e.arr.toSet
      val size = set.size
      (set, size)
    }

    private lazy val combinedSets: Set[JValue] =
      sets.foldLeft(Set.empty[JValue])((a, b) => a ++ b._1)

    /**
     * Check if enum is subset of one of predefined enums
     *
     * @param enum Enum values to check
     * @return true if it is subset
     */
    def isPredefinedEnum(enum: JArray): Boolean =
      getPredefinedEnum(enum).isDefined

    /**
     * Check if any of sets contains value
     *
     * @param value JSON value supposed to be enum value
     * @return true if value can be enum
     */
    def inOneOfEnums(value: JValue): Boolean =
      combinedSets.contains(value)

    /**
     * Get first predefined enum which contains all of `enum` values
     *
     * @param enum Enum values to check
     * @return enum set if it exists
     */
    def getPredefinedEnum(enum: JArray): Option[List[JValue]] = {
      val set = enum.arr.toSet
      sets
        .filter(_._2 >= set.size)   // throw away too small sets
        .find { case (possibleSet, size) => { set.subsetOf(possibleSet) } }
        .map(_._1)
        .map(_.toList)
    }
  }

  /**
   * Transformation function which substiture enum with one of predefined
   * enums if all of it's values are contained in predefined
   *
   * @param context schema context with list of predefined sets
   * @return same schema, but with full predefined enum
   */
  def substituteEnums(implicit context: SchemaContext): PartialFunction[JsonSchema, JsonSchema] = {
    case s @ StringSchema(_, _, _, Some(enum)) => {
      val fullEnum = context.getPredefinedEnum(JArray(enum))
      if (fullEnum.isDefined) { s.copy(enum = fullEnum) } else { s }
    }
    case i @ IntegerSchema(_, _, Some(enum)) => {
      val fullEnum = context.getPredefinedEnum(JArray(enum))
      if (fullEnum.isDefined) { i.copy(enum = fullEnum) } else { i }
    }
    case n @ NumberSchema(_, _, Some(enum)) => {
      val fullEnum = context.getPredefinedEnum(JArray(enum))
      if (fullEnum.isDefined) { n.copy(enum = fullEnum) } else { n }
    }
  }

  /**
   * Recursively get all keys from all objects which schema contains
   *
   * @param schema schema to extract keys from
   * @return set of keys
   */
  def extractKeys(schema: JsonSchema): Set[String] = schema match {
    case ObjectSchema(props) =>
      props.keySet ++ props.flatMap { case (k, v) => extractKeys(v) }
    case ArraySchema(items) =>
      extractKeys(items)
    case ProductSchema(obj, arr, _, _, _, _, _) =>
      obj.map(extractKeys(_)).getOrElse(Set.empty[String]) ++ arr.map(extractKeys(_)).getOrElse(Set.empty[String])
    case _ =>
      Set.empty[String]
  }

  /**
   * Auxiliary class to hold lower and upper bounds for integer ranges
   * Used by ``encaseNumericRange`` function
   *
   * @param minimum lower bound
   * @param maximum upper bound
   */
  private[schema] case class Range(minimum: Option[BigInt], maximum: Option[BigInt])

  /**
   * List of Int ranges sorted by size, used by ``encaseNumericRange`` function
   */
  private[schema] val ranges = List(
    Range(Some(0),             Some(32767)),         // Int16
    Range(Some(-32768),        Some(32767)),
    Range(Some(0),             Some(Int.MaxValue)),  // Int32
    Range(Some(Int.MinValue),  Some(Int.MaxValue)),
    Range(Some(0),             Some(Long.MaxValue)), // Int64
    Range(Some(Long.MinValue), Some(Long.MaxValue))
  )

  /**
   * Pick predefined (Int16, Int32, Int64) [[Range]] for
   * two values. If min bound isn't specified it will pick using only max.
   * If both bound aren't specified blank range will be returned
   * Used by ``encaseNumericRange`` function
   *
   * @param min lower bound
   * @param max upper bound
   * @return [[Range]] for specified bounds
   */
  private[schema] def guessRange(min: Option[BigInt], max: Option[BigInt]): Range = (min, max) match {
    case (Some(minimum), Some(maximum)) =>
      ranges.find(r => r.minimum <= min && r.maximum >= max).getOrElse(Range(None, None))
    case (None, Some(maximum)) =>
      ranges.map(_.maximum).find(r => r.maximum >= max).map(Range(None, _)).getOrElse(Range(None, None))
    case _ =>
      Range(None, None)
  }

  /**
   * Transformation function for numbers and integers, so that their minimum
   * and maximum values will be encased in Int16-64
   * e.g. {min: 10, max: 1000} is positive Int16,
   *      {min: -10, max: 50000} is negative Int32
   *
   * @return transformed schema
   */
  def encaseNumericRange: PartialFunction[JsonSchema, JsonSchema] = {
    case int: IntegerSchema => {
      val range = guessRange(int.minimum, int.maximum)
      int.copy(minimum = range.minimum, maximum = range.maximum)(int.schemaContext)
    }
    case num: NumberSchema => {
      val min = num.minimum.flatMap(x => if (x < 0) None else Some(0.toDouble))
      num.copy(min, None)(num.schemaContext)
    }
  }

  /**
   * Get monoid instance for JsonSchema with specified enum cardinality
   *
   * @param schemaContext context with all information for create and merge
   * @return Monoid instance for merge
   */
  def getMonoid(schemaContext: SchemaContext): Monoid[JsonSchema] = {

    implicit val context = schemaContext

    /**
     * Monoid instance for JSON Schema
     * Declares associative merge operation with specified enum cardinality
     * and `{}` as zero element
     */
    new Monoid[JsonSchema] {
      def zero = ZeroSchema()
      def append(a: JsonSchema, b: => JsonSchema) = {
        a.merge(b)
      }
    }
  }

}
