package com.snowplowanalytics.schemaguru.generators

// Scala
import scala.math.min

// json4s
import org.json4s._
import org.json4s.JsonDSL._

object LevenshteinAnnotator {
  /**
   * Alias type for set of string pairs
   */
  type KeyPairs = Set[Pair[String, String]]

  // Strings shorter than thresholdLength do not comparing
  val thresholdLength = 3

  val thresholdDistance = 3

  /**
   * Actual Levenshtein distance function.
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
   * Convert KeyPairs to it's JSON array representation
   *
   * @param pairs set of key pairs
   * @return JSON Array with unique pairs
   */
  def pairsToJArray(pairs: KeyPairs): JArray = {
    val setOfJArrays: Set[JArray] = pairs.map(pp => {
      if (pp._1 <= pp._2) {      // preserve order, so merge remain associative
        List(JString(pp._1), JString(pp._2))
      } else {
        List(JString(pp._2), JString(pp._1))
      }
    }).map(JArray(_))
    JArray(setOfJArrays.toList)
  }

  /**
   * Annotate unreduced accumulated schema with array of
   * possible duplicates taken from current schema
   *
   * @param currentSchema current schema
   * @param accum unreduced accumulated schema (which will be annotated)
   * @return unreduced schema with array of possible duplicates
   */
  def addPossibleDuplicates(currentSchema: JValue, accum: JValue): JValue = {
    val schKeys = SchemaType.getFrom(currentSchema).map(_.extractAllKeys)
    val accKeys = SchemaType.getFrom(accum).map(_.extractAllKeys)
    val closePairs = (schKeys, accKeys) match {
      case (Some(s), Some(a)) => compareSets(s, a)
      case _ => Set.empty[Pair[String, String]]
    }

    accum match {
      case obj: JObject => {
        val duplicates: JObject = ("possibleDuplicates",  pairsToJArray(closePairs))
        obj.merge((duplicates))
      }
      case _ => accum
    }
  }

  /**
   * Helper function for producing all possible pairs
   * for Strings with length > 3
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
   * Calculate Levenshtein for all possible combinations of elements of two sets
   * @param schKeys first set
   * @param accKeys second set
   * @return pairs which distance threshold is lower than specified
   */
  def compareSets(schKeys: Set[String], accKeys: Set[String]): KeyPairs = {
    crossProduct(schKeys, accKeys).flatMap(s => {
      val distance = calculateDistance(s._1, s._2)
      if (distance == 0 || distance > thresholdDistance) Set.empty[Pair[String, String]]
      else Set((s._1, s._2))
    })
  }
}
