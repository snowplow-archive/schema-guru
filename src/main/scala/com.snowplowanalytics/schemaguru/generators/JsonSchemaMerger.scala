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
package generators

// Scalaz
import scalaz._
import Scalaz._

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.pretty

// This library
import json.SchemaHelpers._


/**
 * Takes a list of JsonSchemas and merges them together into
 * a master schema which will validate any of the sub schemas.
 */
object JsonSchemaMerger {
  /**
   * Merges a pair of JSON Schemas represented by
   * JValues. The output JSON Schema will validate
   * any instance validated by either schema.
   *
   * @param left The first JSON Schema
   * @param right The second JSON Schema
   * @return the JSON Schema which is the superset of
   *         both input JSON Schemas
   */
  def merge2(left: JValue, right: JValue): JValue =
    reduceMergedSchema(
      formatSchemaForMerge(left) merge formatSchemaForMerge(right)
    )

  /**
   * Merges a (possibly empty) collection of JSON Schemas
   * represented by JValues. The output JSON Schema
   * will validate any instance validated by any of
   * the schemas in the collection.
   *
   * @param iter The collection of JSON Schemas we
   *        want to merge
   * @return the JSON Schema which is the superset of
   *         all input JSON Schemas
   */
  def mergeN(iter: TraversableOnce[JValue]): Option[JValue] =
    for {
      s <- iter.reduceLeftOption {
        formatSchemaForMerge(_) merge formatSchemaForMerge(_)
      }
    } yield reduceMergedSchema(s)

  /**
   * Merges a list of JsonSchemas together into one
   *
   * @param jsonSchemaList The list of Schemas which
   *        we want to merge
   * @param enumCardinality cardinality for detecting possible enums
   * @return the cumulative JsonSchema
   */
  // TODO: handle case where _starting_ List is empty (see: reduceLeftOption above)
  def mergeJsonSchemas(jsonSchemaList: List[JValue], accum: JValue = Nil, enumCardinality: Int = 0): JValue = {
    jsonSchemaList match {
      case x :: xs => {
        val annotatedAcc = LevenshteinAnnotator.addPossibleDuplicates(x, accum)
        mergeJsonSchemas(xs, formatSchemaForMerge(x).merge(annotatedAcc), enumCardinality)
      }
      case Nil     => reduceMergedSchema(accum, enumCardinality)
    }
  }


  /**
   * Transforms the type descriptor for every key
   * in the JsonSchema into an array so that we
   * can merge JsonSchemas together.
   * 
   * @param jsonSchema The JsonSchema which we are
   *        preparing for a merge
   * @return the formatted JsonSchema ready for merge
   */
  // TODO: we can significantly decrease memory consumption by filtering data on this step
  def formatSchemaForMerge(jsonSchema: JValue): JValue =
    jsonSchema transformField {
      case ("type", JObject(v))     => ("type", JObject(v))
      case ("type", JString(v))     => ("type", JArray(List(v)))

      case ("minimum", JDouble(m))  => ("minimum", JArray(List(m)))
      case ("minimum", JInt(m))     => ("minimum", JArray(List(m)))
      case ("maximum", JInt(m))     => ("maximum", JArray(List(m)))

      case ("format", JString(f))   => ("format", JArray(List(f)))
      case ("format", JNothing)     => ("format", JArray(List(JNothing)))

      case ("pattern", JString(f))  => ("pattern", JArray(List(f)))
      case ("pattern", JNothing)    => ("pattern", JArray(List(JNothing)))

      case ("items", JArray(items)) => ("items", mergeJsonSchemas(items))
    }

  /**
   * Reduces merged representation of schema to single schema.
   * reduceMergedSchema produce valid JSON schema, but annotated
   * with auxiliary data (like possibleDuplicates), which should
   * be removed in further steps.
   *
   * i.e. "type" : ["string"] -> "type" : "string"
   *      "type" : ["integer", "number"] -> "type" : "number"
   *      "maximum" : [0, 10] -> "maximum" : 10
   *
   * @param jsonSchema The Schema we now want to reduce
   * @param enumCardinality cardinality for detecting possible enums
   * @return the reduced JsonSchema ready for publishing
   */
  def reduceMergedSchema(jsonSchema: JValue, enumCardinality: Int = 0): JValue = {
    jsonSchema transformField {
      case ("type", JArray(list)) =>
        ("type", list match {
          case list if list.size == 1       => list(0)
          case list if isMergedNumber(list) => list.filterNot(_ == JString("integer"))
          case list                         => JArray(list)
        })
      case ("properties", properties) =>
        ("properties", properties map(reduceIntegerFieldRange)
                                  map(reduceNumberFieldRange)
                                  map(reduceStringField("format"))
                                  map(reduceStringField("pattern")))
      case ("enum", JArray(list)) if list.length > enumCardinality =>
        ("enum", JNothing) // delete it
    }
  }
}
