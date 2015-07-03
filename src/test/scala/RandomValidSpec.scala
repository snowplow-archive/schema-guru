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

// Java
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import org.joda.time.DateTime

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// Testing
import org.scalacheck._
import org.specs2.{Specification, ScalaCheck}

// This project
import JsonSchemaGenerator.jsonToSchema

class RandomValidSpecification extends Specification with ScalaCheck with JsonGen { def is = s2"""
  Derive schema from random generated JSON and validate against itself
    validate random JSON against derived schema            $validateJsonAgainstDerivedSchema
    validate any JSON against empty schema                 $validateJsonAgainstEmptySchema
    fail to validate mismatched (Int/String) key           $validateAgainstWrongSchema
  """

  def validateJsonAgainstDerivedSchema = prop { (json: JValue) =>
    val factory: JsonSchemaFactory  = JsonSchemaFactory.byDefault()
    val jss = jsonToSchema(json)
    val derivedSchema = asJsonNode(jss)
    val schema: JsonSchema = factory.getJsonSchema(derivedSchema)

    schema.validate(asJsonNode(json)).isSuccess must beTrue
  }.set(maxSize = 20)

  def validateJsonAgainstEmptySchema = prop { (json: JValue) =>
    val factory: JsonSchemaFactory  = JsonSchemaFactory.byDefault()
    val emptySchema = asJsonNode(parse("{}"))
    val schema: JsonSchema = factory.getJsonSchema(emptySchema)

    schema.validate(asJsonNode(json)).isSuccess must beTrue
  }.set(maxSize = 15)

  def validateAgainstWrongSchema = Prop.forAll(generateJsonWithKeys(Map("mismatched_key" -> arbitraryJInt)),
                                               generateJsonWithKeys(Map("mismatched_key" -> arbitraryJString))) {
    (jsonForSchema: JValue, json: JValue) =>
    val factory: JsonSchemaFactory = JsonSchemaFactory.byDefault()
    val derivedInvalidSchema = asJsonNode(jsonToSchema(jsonForSchema))
    val invalidSchema: JsonSchema = factory.getJsonSchema(derivedInvalidSchema)

    invalidSchema.validate(asJsonNode(json)).isSuccess must beFalse
  }
}

/**
 * Trait responsible for arbitrary JSON
 */
trait JsonGen {
  implicit def arbitraryJsonType: Arbitrary[JValue] =
    Arbitrary { Gen.sized(depth => jsonType(depth)) }

  def arbitraryIsoDate: Arbitrary[String] =
    Arbitrary(Gen.choose(0L, 1922659200L * 1000).map(new DateTime(_).toString))

  def jsonType(depth: Int): Gen[JValue] =
    Gen.oneOf(jsonArray(depth), jsonObject(depth))

  def jsonArray(depth: Int): Gen[JArray] = for {
    n    <- Gen.choose(1, 4)
    vals <- values(n, depth)
  } yield JArray(vals)

  def jsonObject(depth: Int): Gen[JObject] = for {
    n <- Gen.choose(1, 4)
    ks <- keys(n)
    vals <- values(n, depth)
  } yield JObject((ks zip vals):_*)

  def keys(n: Int): Gen[List[String]] =
    Gen.listOfN(n, Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString))

  def values(n: Int, depth: Int) =
    Gen.listOfN(n, value(depth))

  def value(depth: Int): Gen[JValue] =
    if (depth == 0) { terminalType }
    else { Gen.oneOf(jsonType(depth - 1), terminalType) }

  def terminalType: Gen[JValue] = Gen.oneOf(
    arbitraryJString,
    arbitraryJInt,
    arbitraryJBool,
    arbitraryJUuid,
    arbitraryJIsoDate
  )

  def arbitraryJString =
    Gen.listOf(Gen.alphaChar).map(_.mkString).suchThat(_.forall(_.isLetter)).map(JString(_))

  def arbitraryJInt =
    Arbitrary.arbitrary[Int].map(JInt(_))

  def arbitraryJBool =
    Arbitrary.arbitrary[Boolean].map(JBool(_))

  def arbitraryJUuid =
    Gen.uuid.map(x => JString(x.toString))

  def arbitraryJIsoDate =
    Arbitrary.arbitrary(arbitraryIsoDate).map(JString(_))

  def arbitraryJSmallInt =
    Gen.choose(-32768, 32767).map(JInt(_))

  def arbitraryJBigInt =
    Arbitrary.arbitrary[BigInt].map(JInt(_))

  /**
   * Generates JSON with predefined keys and specified types
   * 
   * @param typeMap map of key to arbitrary JValues
   * @return arbitrary JSON with predefined keys and arbitrary values
   */
  def generateJsonWithKeys(typeMap: Map[String, Gen[JValue]]): Gen[JObject] = {
    val mapWithGeneratedValues = for {
      (k: String, v: Gen[JValue]) <- typeMap
      vl <- v.sample
    } yield (k, vl)
    val jObject = JObject(mapWithGeneratedValues.toList)
    Gen.const(jObject)
  }
}
