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
package json

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// Testing
import org.specs2.Specification

class IntegerRangeReduceSpec extends Specification with SchemaIntegerHelper { def is = s2"""
  Check integer range
    guess zero as positive                     $guessZero
    guess Int16                                $guessInt16
    guess negative Int32                       $guessInt32Negative
    guess Int64                                $guessInt64
    check Int16 as Short                       $checkInt16Range
    check Int32 as Int                         $checkInt32Range
    check Int64 as Long                        $checkInt64Range
    """

  val int16Range = Range(-32768, 32767)   // SchemaIntegerHelper.Range, not Scala built-in
  val int32Range = Range(-2147483648, 2147483647)
  val int64Range = Range(-9223372036854775808L, 9223372036854775807L)

  val IntegerT  = JObject(List(("type", JString("integer"))))
  val IntegerTWithInt16Range = JObject(List(("type", JString("integer")), ("minimum", JInt(Short.MinValue: Int)), ("maximum", JInt(Short.MaxValue: Int))))
  val IntegerTWithInt64Range = JObject(List(("type", JString("integer")), ("minimum", JInt(Long.MinValue: BigInt)), ("maximum", JInt(Long.MaxValue: BigInt))))

  val schemaWithPositiveInt16 = parse("""{"type": "integer", "minimum": [21, 100, 0, 31], "maximum": [30000, 16000, 100]}""")

  def guessZero =
    IntegerFieldReducer(List(0), List(0)).minimumBound must beEqualTo(0)

  def guessInt16 =
    IntegerFieldReducer(List(-1), List(31000)).minimumBound must beEqualTo(int16Range.minimum)

  def guessInt32Negative =
    IntegerFieldReducer(List(-34000), List(3000)).minimumBound must beEqualTo(int32Range.minimum)

  def guessInt64 =
    IntegerFieldReducer(List(-34000), List(9223372036854775806L)).minimumBound must beEqualTo(int64Range.minimum)

  def checkInt16Range =
    int16Range must beEqualTo(Range(Short.MinValue.toInt, Short.MaxValue.toInt))

  def checkInt32Range =
    int32Range must beEqualTo(Range(Int.MinValue, Int.MaxValue))

  def checkInt64Range =
    int64Range must beEqualTo(Range(Long.MinValue, Long.MaxValue))
}

