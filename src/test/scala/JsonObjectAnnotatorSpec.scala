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

// Scala
import scala.io.Source

// specs2
import org.specs2.Specification
import org.specs2.matcher.JsonMatchers

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

// This library
import SchemaType._

class JsonObjectAnnotatorSpec extends Specification with JsonMatchers { def is = s2"""
  Check JsonObjectAnnotator
    extract all nested keys     $extractAllKeys
    extract object              $extractObject
    extract primitive types     $extractPrimitiveTypes
  """

  val schemaWithObjectJson: JObject =
    ("type" -> List("string", "object")) ~    // object has higher priority
    ("properties" ->
      ("key" ->
        ("type" -> "string"))) ~
    ("additionalProperties" -> false)

  val schemaWithPrimitiveTypesJson: JObject =
    ("type" -> List("string", "number")) ~
    ("format" -> "date-time")

  def extractAllKeys = {
    val keys: Set[String] = Set(
      "firstObject", "someNestedObjects", "someObject", "propertyTwo",
      "hereNestedAgain", "propertyOne", "secondObject", "itShouldBeNull",
      "properties", "thirdObject", "firstKey", "nowKey",
      "nestedObjectWithSpecialKeys", "type", "nowObject", "one",
      "arrayOfObjects")
    val json = parse(Source.fromURL(getClass.getResource("/duplicates/schema_with_many_nested_objects.json")).mkString)
    getFrom(json).map(_.extractAllKeys) must beSome[Set[String]](keys)
  }

  def extractObject = {
    val schemaWithObject = ObjectType(Map("key" -> ("type", "string")), false, List("string", "object"))
    getFrom(schemaWithObjectJson) must beSome(schemaWithObject)
  }

  def extractPrimitiveTypes = {
    val schemaWithPrimitiveTypes = PrimitiveType(List("string", "number"))
    getFrom(schemaWithPrimitiveTypesJson) must beSome(schemaWithPrimitiveTypes)
  }
}
