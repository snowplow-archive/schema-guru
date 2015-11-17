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

// scala
import scala.util.parsing.json.JSONArray

// specs2
import org.specs2.Specification
import org.specs2.matcher.JsonMatchers

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// This library
import schema.JsonSchema
import schema.types._
import schema.Helpers._

class EnumSpec extends Specification with JsonMatchers { def is = s2"""
  Check enum detection
    merge with zero cardinality                 $mergeWithoutEnum
    merge with same value                       $mergeWithSameEnum
    merge several enums                         $mergeSeveralEnums
    merge 3 enums with 2 cardinality            $mergeWithOverCardinality
  """

  val enum0Ctx = SchemaContext(0)
  val enum2Ctx = SchemaContext(2)
  val enum5Ctx = SchemaContext(5)

  val enum1 = List(JString("AB"))
  val enum2 = List(JString("AC"))
  val enum3 = List(JString("AD"))

  val enum1Schema = StringSchema(enum = Some(enum1))(enum2Ctx)
  val enum2Schema = StringSchema(enum = Some(enum2))(enum2Ctx)
  val enum3Schema = StringSchema(enum = Some(enum3))(enum2Ctx)

  def mergeWithoutEnum = {
    implicit val monoid = getMonoid(enum0Ctx)
    val schemas: List[JsonSchema] = List(enum1Schema, enum1Schema)
    val json = compact(schemas.suml.toJson)
    json must not */("enum" -> ".*".r)
  }

  def mergeWithSameEnum = {
    implicit val monoid = getMonoid(enum5Ctx)
    val schemas: List[JsonSchema] = List(enum1Schema, enum1Schema)
    val json = pretty(schemas.suml.toJson)
    json must /("enum" -> JSONArray(List("AB")))
  }

  def mergeSeveralEnums = {
    implicit val monoid = getMonoid(enum5Ctx)
    val schemas: List[JsonSchema] = List(enum1Schema, enum1Schema, enum2Schema, enum3Schema)
    val json = compact(schemas.suml.toJson)
    json must /("enum" -> JSONArray(List("AB", "AC", "AD"))) // TODO: ignore order
  }

  def mergeWithOverCardinality = {
    implicit val monoid = getMonoid(enum2Ctx)
    val schemas: List[JsonSchema] = List(enum1Schema, enum1Schema, enum2Schema, enum3Schema)
    val json = compact(schemas.suml.toJson)
    json must not */("enum" -> ".*".r)
  }
}
