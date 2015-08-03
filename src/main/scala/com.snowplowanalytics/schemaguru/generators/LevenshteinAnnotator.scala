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
import scala.math.min

object LevenshteinAnnotator {
  /**
   * Alias type for set of string pairs
   */
  type KeyPairs = Set[(String, String)]

  // Strings shorter than thresholdLength are not compared
  val thresholdLength = 3

  // Pair of strings which uniform representation (see below) forms has
  // distance less than thresholdDistance can be considered as probably
  // duplicated
  val thresholdDistance = 1

  /**
   * Get all pairs of possible duplicated keys
   *
   * @param keys set of strings
   * @return pair of every similar entry
   */
  def getDuplicates(keys: Set[String]): KeyPairs =
    compareSets(keys, keys) map { case (first, second) =>
      if (first <= second) {
        (first, second)
      } else {
        (second, first)
      }
    }

  /**
   * Levenshtein distance function.
   * Measuring the difference between two sequences (strings in our case)
   *
   * @param a first sequence
   * @param b second sequence
   * @return value indicating distance
   */
  def calculateDistance[A](a: Iterable[A], b: Iterable[A]): Int = {
    ((0 to b.size).toList /: a)((prev, x) =>
      (prev zip prev.tail zip b).scanLeft(prev.head + 1) {
        case (h, ((d, v), y)) => min(min(h + 1, v + 1), d + (if (x == y) 0 else 1))
      }).last
  }

  /**
   * Helper function for producing all possible pairs for Strings with
   * length > ``thresholdLength``
   * Ex: (a, b),(d, e) = (a,d),(a,e)(b,d),(b,e)
   *
   * @param xs set of strings
   * @param ys other set of strings
   * @return set of all possible pairs
   */
  def crossProduct(xs: Set[String], ys: Set[String]): KeyPairs = {
    for {
      x <- xs
      y <- ys
      if (x.length > thresholdLength && y.length > thresholdLength) // it doesn't make sense to compare short keys
    } yield (x, y)
  }

  /**
   * Eliminates all underscores and hyphens and lowercase whole string
   * to handle special cases like sameKey same_key
   *
   * @param string string to be transformed
   * @return lowercased string without some special characters
   */
  def uniformString(string: String): String = {
    string.replaceAll("[\\-,\\_]", "").toLowerCase
  }

  /**
   * Calculate Levenshtein for all possible combinations of elements of two sets
   * and returns pairs which are seems similar
   *
   * @param schKeys first set
   * @param accKeys second set
   * @return pairs which distance threshold is lower than specified
   */
  def compareSets(schKeys: Set[String], accKeys: Set[String]): KeyPairs = {
    crossProduct(schKeys, accKeys).flatMap { case (first, second) => {
      if (first != second) {
        val uniformedFirst = uniformString(first)
        val uniformedSecond = uniformString(second)
        val distance = calculateDistance(uniformedFirst, uniformedSecond)
        if (distance <= thresholdDistance) {
          Set((first, second))
        } else {
          Set.empty[(String, String)]
        }
      } else {
        Set.empty[(String, String)]
      }
    }}
  }
}
