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

// Scala
import scala.io.Source

// specs2
import org.specs2.Specification
import org.specs2.matcher.JsonMatchers
import org.specs2.scalaz.ValidationMatchers


// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

// This library
import generators.SchemaGenerator

class JsonObjectAnnotatorSpec extends Specification with JsonMatchers with ValidationMatchers { def is = s2"""
  Check JsonObjectAnnotator
    extract all nested keys     $extractAllKeys
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
    val context = Helpers.SchemaContext(0)
    val generator = SchemaGenerator(context)

    val keys = Set(
      "firstKey", "secondKey", "firstObject", "firstObjectWithKeys", "id",
      "nullable", "firstArray", "firstArrayWithObjects", "arrId", "type",
      "firstDeepObject", "firstLevel", "againFirst", "someMoreLevels",
      "somethingIn", "anotherKeyIn", "nothingSpecial", "neighbor",
      "verySpecial", "deepToo", "deepArray", "hello", "boom")
    val json = parse(Source.fromURL(getClass.getResource("/duplicates/schema_with_many_nested_objects.json")).mkString)
    val schema = generator.jsonToSchema(json)
    schema.map(Helpers.extractKeys(_)) must beSuccessful(keys)
  }
}
