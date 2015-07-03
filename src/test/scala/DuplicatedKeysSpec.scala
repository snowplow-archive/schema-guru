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
import scala.util.parsing.json.JSONArray

// specs2
import org.specs2.Specification
import org.specs2.matcher.JsonMatchers

// json4s
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._

// This library
import LevenshteinAnnotator._

class DuplicatedKeysSpec extends Specification with JsonMatchers { def is = s2"""
  Check enum detection
    merge with zero cardinality                 $calculateLevenshteinDistance
    merge with zero cardinality                 $calculateLevenshteinDistance1
    merge with zero cardinality                 $calculateLevenshteinDistance2
    merge with zero cardinality                 $eliminateIdenticalPairs
    merge with zero cardinality                 $mergeSimilarPairs
    merge with zero cardinality                 $mergeSchemasWithDuplicates
  """

  def calculateLevenshteinDistance = {
    calculateDistance("someKey", "somekey") must beEqualTo(1)
  }

  def calculateLevenshteinDistance1 = {
    calculateDistance("someKey", "some_Key") must beEqualTo(1)
  }

  def calculateLevenshteinDistance2 = {
    calculateDistance("sameKey", "sameKey") must beEqualTo(0)
  }

  def eliminateIdenticalPairs = {
    val result = JArray(List(JArray(List(JString("one"), JString("two")))))
    pairsToJArray(Set(("one", "two"), ("two", "one"), ("one", "two"))) must beEqualTo(result)
  }

  def mergeSimilarPairs = {
    val result = JArray(List(JArray(List(JString("one"), JString("two"))), JArray(List(JString("one"), JString("to")))))
    pairsToJArray(Set(("one", "two"), ("to", "one"))) must beEqualTo(result)
  }

  def mergeSchemasWithDuplicates = {
    val json = Source.fromURL(getClass.getResource("/duplicates/schema1.json")).mkString
    json must /("possibleDuplicates" -> JSONArray(List(JSONArray(List("arbitraryKey", "arbitaryKey")))))
  }
}

