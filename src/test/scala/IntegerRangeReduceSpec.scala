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

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// Testing
import org.specs2.Specification

class IntegerRangeReduceSpec extends Specification { def is = s2"""
  Check integer range
    guess zero as positive                     $guessZero
    guess Int16                                $guessInt16
    guess negative Int32                       $guessInt32Negative
    guess Int64                                $guessInt64
    check Int16 as Short                       $checkInt16Range
    check Int32 as Int                         $checkInt32Range
    check Int64 as Long                        $checkInt64Range
    """

  val int16Range = Helpers.Range(Some(-32768), Some(32767))   // not Scala built-in Range
  val int32Range = Helpers.Range(Some(-2147483648), Some(2147483647))
  val int64Range = Helpers.Range(Some(-9223372036854775808L), Some(9223372036854775807L))

  val IntegerT  = JObject(List(("type", JString("integer"))))
  val IntegerTWithInt16Range = JObject(List(("type", JString("integer")), ("minimum", JInt(Short.MinValue: Int)), ("maximum", JInt(Short.MaxValue: Int))))
  val IntegerTWithInt64Range = JObject(List(("type", JString("integer")), ("minimum", JInt(Long.MinValue: BigInt)), ("maximum", JInt(Long.MaxValue: BigInt))))

  val schemaWithPositiveInt16 = parse("""{"type": "integer", "minimum": [21, 100, 0, 31], "maximum": [30000, 16000, 100]}""")

  def guessZero =
    Helpers.guessRange(Some(0), Some(0)).minimum must beSome(0)

  def guessInt16 =
    Helpers.guessRange(Some(-1), Some(31000)).minimum must beEqualTo(int16Range.minimum)

  def guessInt32Negative =
    Helpers.guessRange(Some(-34000), Some(3000)).minimum must beEqualTo(int32Range.minimum)

  def guessInt64 =
    Helpers.guessRange(Some(-34000), Some(9223372036854775806L)).minimum must beEqualTo(int64Range.minimum)

  def checkInt16Range =
    int16Range must beEqualTo(Helpers.Range(Some(Short.MinValue.toInt), Some(Short.MaxValue.toInt)))

  def checkInt32Range =
    int32Range must beEqualTo(Helpers.Range(Some(Int.MinValue), Some(Int.MaxValue)))

  def checkInt64Range =
    int64Range must beEqualTo(Helpers.Range(Some(Long.MinValue), Some(Long.MaxValue)))
}

