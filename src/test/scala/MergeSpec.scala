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

// json4s
import org.json4s._

// Testing
import org.specs2.Specification

// This library
import schema.types._
import schema.Helpers.SchemaContext

class MergeSpec extends Specification { def is = s2"""
  Check integer merge
    maintain all types in array                            $maintainTypesInArray
    merge two instances                                    $mergeMinimumValuesForInt32
    merge integer with number must result in number        $mergeIntegerWithNumber
    merge two distinct string formats                      $mergeDistinctFormats
    merge strings with and without format                  $mergeStringWithFormatAndWithout
    merge two different types produce product              $mergeTwoDifferentTypes
    reduce properties for product types                    $reducePropertiesForProductType
    merge strings with maxLengths                          $mergeStringsWithMaxLengths
    merge strings with minLengths                          $mergeStringsWithMinLengths
    merge product types with maxLengths                    $mergeProductTypeWithMaxLengths
    """

  implicit val formats = DefaultFormats

  implicit val ctx = SchemaContext(0)

  val StringS = StringSchema()
  val IntegerS = IntegerSchema()

  val StringWithLengths = StringSchema(minLength = Some(3), maxLength = Some(10))
  val StringWithLengths2 = StringSchema(minLength = Some(5), maxLength = Some(8))

  val schemaWithInt16 = ObjectSchema(Map("test_key" -> IntegerSchema(Some(-2), Some(3))))
  val schemaWithInt32 = ObjectSchema(Map("test_key" -> IntegerSchema(Some(-34000), Some(3))))
  val schemaWithNumber = ObjectSchema(Map("test_key" -> NumberSchema(Some(-34000), Some(3.3))))

  val schemaWithUuid = ObjectSchema(Map("test_key" -> StringSchema(format = Some("uuid"))))
  val schemaWithDateTime = ObjectSchema(Map("test_key" -> StringSchema(format = Some("date-time"))))
  val schemaWithoutFormat = ObjectSchema(Map("test_key" -> StringSchema()))

  def maintainTypesInArray =
    StringS.merge(IntegerS) must beEqualTo(ProductSchema(stringSchema = Some(StringS), integerSchema = Some(IntegerS)))

  def mergeMinimumValuesForInt32 = {
    val merged = schemaWithInt16.merge(schemaWithInt32).toJson
    (merged \ "properties" \ "test_key" \ "minimum").extract[BigInt] must beEqualTo(-34000)
  }

  def mergeIntegerWithNumber = {
    val merged = schemaWithInt32.merge(schemaWithNumber).toJson
    (merged \ "properties" \ "test_key" \ "type").extract[String] must beEqualTo("number")
  }

  def mergeDistinctFormats = {
    val merged = schemaWithUuid.merge(schemaWithDateTime).toJson
    (merged \ "properties" \ "test_key" \ "format").extract[Option[String]] must beNone
  }

  def mergeStringWithFormatAndWithout = {
    val merged = schemaWithoutFormat.merge(schemaWithDateTime).toJson
    (merged \ "properties" \ "test_key" \ "format").extract[Option[String]] must beNone
  }

  def mergeTwoDifferentTypes = {
    val merged = schemaWithDateTime.merge(schemaWithInt16).toJson
    (merged \ "properties" \ "test_key" \ "type").extract[List[String]].sorted must beEqualTo(List("integer", "string"))
  }

  def reducePropertiesForProductType = {
    val merged = schemaWithDateTime.merge(schemaWithInt16).toJson
    (merged \ "properties" \ "test_key" \ "format").extract[String] mustEqual("date-time")
  }

  def mergeStringsWithMaxLengths = {
    val merged = StringWithLengths.merge(StringWithLengths2).toJson
    (merged \ "maxLength").extract[Int] mustEqual(10)
  }

  def mergeStringsWithMinLengths = {
    val merged = StringWithLengths2.merge(StringWithLengths).toJson
    (merged \ "minLength").extract[Int] mustEqual(3)
  }

  def mergeProductTypeWithMaxLengths = {
    val merged = IntegerS.merge(StringWithLengths2.merge(StringWithLengths)).toJson
    (merged \ "maxLength").extract[Int] mustEqual(10)
  }
}
