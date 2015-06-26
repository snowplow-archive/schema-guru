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

// scala
import scala.util.parsing.json.JSONArray

// specs2
import org.specs2.Specification
import org.specs2.matcher.JsonMatchers

// json4s
import org.json4s.jackson.JsonMethods._

// This library
import JsonSchemaMerger._

class EnumSpec extends Specification with JsonMatchers { def is = s2"""
  Check enum detection
    merge with zero cardinality                 $mergeWithoutEnum
    merge with same value                       $mergeWithSameEnum
    merge several enums                         $mergeSeveralEnums
    merge 3 enums with 2 cardinality            $mergeWithOverCardinality
  """

  val enum1Schema = parse("""{"type": "object","properties": {"address": {"type": "string","enum": ["AB"]}}}""")
  val enum2Schema = parse("""{"type": "object","properties": {"address": {"type": "string","enum": ["AC"]}}}""")
  val enum3Schema = parse("""{"type": "object","properties": {"address": {"type": "string","enum": ["AD"]}}}""")

  def mergeWithoutEnum = {
    val json = compact(mergeJsonSchemas(List(enum1Schema, enum1Schema), enumCardinality = 0))
    json must not */("enum" -> ".*".r)
  }

  def mergeWithSameEnum = {
    val json = compact(mergeJsonSchemas(List(enum1Schema, enum1Schema), enumCardinality = 5))
    json must /("properties") /("address") /("enum" -> JSONArray(List("AB")))
  }

  def mergeSeveralEnums = {
    val json = compact(mergeJsonSchemas(List(enum1Schema, enum1Schema, enum2Schema, enum3Schema), enumCardinality = 5))
    json must /("properties") /("address") /("enum" -> JSONArray(List("AD", "AC", "AB"))) // TODO: ignore order
  }

  def mergeWithOverCardinality = {
    val json = compact(mergeJsonSchemas(List(enum1Schema, enum1Schema, enum2Schema, enum3Schema), enumCardinality = 2))
    json must not */("enum" -> ".*".r)
  }
}
