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
import org.json4s.JsonDSL._

// Testing
import org.specs2.Specification

// This project
import JsonSchemaMerger.mergeJsonSchemas

class MergeSpec extends Specification { def is = s2"""
  Check integer merge
    maintain all types in array                            $maintainTypesInArray
    merge maximum values                                   $mergeMaximumValues
    merge minimum values                                   $mergeMinimumValues
    merge two instances                                    $mergeMinimumValuesForInt32
    merge integer with number must result in number        $mergeIntegerWithNumber
    merge two distinct string formats                      $mergeDistinctFormats
    merge strings with and without format                  $mergeStringWithFormatAndWithout
    merge two different types produce product              $mergeTwoDifferentTypes
    reduce properties for product types                    $reducePropertiesForProductType
    """

  implicit val formats = DefaultFormats

  val StringT: JObject = ("type" -> "string")
  val IntegerT: JObject = ("type" -> "integer")
  val DecimalT: JObject = ("type" -> "number")
  val jObjectWithInt16: JObject = ("properties", ("test_key", IntegerT ~ ("maximum", JInt(3)) ~ ("minimum", JInt(-2))))
  val jObjectWithInt32: JObject = ("properties", ("test_key", IntegerT ~ ("maximum", JInt(3)) ~ ("minimum", JInt(-34000))))
  val jObjectWithNumber: JObject = ("properties", ("test_key", DecimalT ~ ("maximum", JDecimal(3.3)) ~ ("minimum", JInt(-34000))))

  val jObjectWithUuid: JObject = ("properties", ("test_key", StringT ~ ("format", JString("uuid"))))
  val jObjectWithDateTime: JObject = ("properties", ("test_key", StringT ~ ("format", JString("date-time"))))
  val jObjectWithoutFormat: JObject = ("properties", ("test_key", StringT ~ ("format", None)))

  def maintainTypesInArray = {
    val merged = mergeJsonSchemas(List(StringT, StringT, StringT, IntegerT, StringT))
    (merged \ "type").extract[List[String]] must beEqualTo(List("string", "integer"))
  }

  def mergeMaximumValues = {
    val merged = mergeJsonSchemas(List(jObjectWithInt16))
    (merged\ "properties" \ "test_key" \ "maximum").extract[BigInt] must beEqualTo(32767)
  }

  def mergeMinimumValues = {
    val merged = mergeJsonSchemas(List(jObjectWithInt16))
    (merged\ "properties" \ "test_key" \ "minimum").extract[BigInt] must beEqualTo(-32768)
  }

  def mergeMinimumValuesForInt32 = {
    val merged = mergeJsonSchemas(List(jObjectWithInt16, jObjectWithInt32))
    (merged \ "properties" \ "test_key" \ "minimum").extract[BigInt] must beEqualTo(-2147483648)
  }

  def mergeIntegerWithNumber = {
    val merged = mergeJsonSchemas(List(jObjectWithInt32, jObjectWithNumber))
    // TODO: should be plain string, not an array
    (merged \ "properties" \ "test_key" \ "type").extract[List[String]] must beEqualTo(List("number"))
  }

  def mergeDistinctFormats = {
    val merged = mergeJsonSchemas(List(jObjectWithUuid, jObjectWithDateTime))
    (merged \ "properties" \ "test_key" \ "format").extract[Option[String]] must beNone
  }

  def mergeStringWithFormatAndWithout = {
    val merged = mergeJsonSchemas(List(jObjectWithoutFormat, jObjectWithDateTime))
    (merged \ "properties" \ "test_key" \ "format").extract[Option[String]] must beNone
  }

  def mergeTwoDifferentTypes = {
    val merged = mergeJsonSchemas(List(jObjectWithDateTime, jObjectWithInt16))
    (merged \ "properties" \ "test_key" \ "type").extract[List[String]].sorted must beEqualTo(List("integer", "string"))
  }

  def reducePropertiesForProductType = {
    val merged = mergeJsonSchemas(List(jObjectWithDateTime, jObjectWithInt16))
    // unreduced property would remain list
    (merged \ "properties" \ "test_key" \ "format").extract[String] mustEqual("date-time")
  }
}
