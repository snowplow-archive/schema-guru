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

// specs2
import org.specs2.Specification

// This library
import LevenshteinAnnotator._

class DuplicatedKeysSpec extends Specification  { def is = s2"""
  Check enum detection
    calculate distance for different cases            $calculateLevenshteinDistance
    calculate distance for snake case                 $calculateLevenshteinDistance1
    distance for identical strings is 0               $calculateLevenshteinDistance2
    cross product for short (less 4) key is empty     $crossProductForShortKeys
    cross product for one and two keys                $crossProductForOneAndTwoKeys
    handle snake_case and camelCase                   $handleSnakeAndCamelCases
    skip short (less 4) keys                          $dontHandleShortKeys
    handle one typo                                   $handleOneTypo
    skip two typos (distance > 1)                     $dontHandleTwoTypos
    handle typos in three keys                        $handleTyposInThreeKeys
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

  def crossProductForShortKeys = {
    crossProduct(Set("aaa"), Set("bbbb")) must beEqualTo(Set())
  }

  def crossProductForOneAndTwoKeys = {
    crossProduct(Set("aaaa"), Set("bbbb", "cccc")) must beEqualTo(Set(("aaaa", "bbbb"), ("aaaa", "cccc")))
  }

  def handleSnakeAndCamelCases = {
    getDuplicates(Set("differentCase", "different_case")) must beEqualTo(Set(("differentCase", "different_case")))
  }

  def dontHandleShortKeys = {
    getDuplicates(Set("short", "sho")) must beEqualTo(Set())
  }

  def handleOneTypo = {
    getDuplicates(Set("oneTypo", "oneType")) must beEqualTo(Set(("oneType", "oneTypo")))
  }

  def dontHandleTwoTypos = {
    getDuplicates(Set("twoTypos", "twoTyped")) must beEqualTo(Set())
  }

  def handleTyposInThreeKeys = {
    getDuplicates(Set("oneTypo", "oneType", "oneTipo")) must beEqualTo(Set(("oneType", "oneTypo"), ("oneTipo", "oneTypo")))
  }
}

