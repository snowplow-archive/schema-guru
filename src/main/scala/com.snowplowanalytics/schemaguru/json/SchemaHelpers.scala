package com.snowplowanalytics.schemaguru.json

import scala.language.implicitConversions

import org.json4s._

object SchemaHelpers {
  /**
   * Filters and extracts list of Ints from List of JValues
   */
  implicit def extractIntsFromJValues(jValues: List[JValue]): List[BigInt] =
    for (jValue <- jValues; JInt(int) <- jValue) yield int

  private case class Range(minimum: BigInt, maximum: BigInt)

  // List of Int ranges sorted by size
  private val ranges = List(
    Range(0, 32767),                Range(-32768, 32767),
    Range(0, 2147483647),           Range(-2147483648, 2147483647),
    Range(0, 9223372036854775807L), Range(-9223372036854775808L, 9223372036854775807L)
  )

  private def guessRange(min: BigInt, max: BigInt) =
    ranges.find(r => r.minimum <= min && r.maximum >= max)

  /**
   * Holds all information about merged JSON Schema Int
   *
   * @param minimums list of all encountered maximum values
   * @param maximums list of all encountered minimum values
   */
  case class IntegerFieldReducer(minimums: List[BigInt], maximums: List[BigInt]) {
    private val minimum = minimums.min
    private val maximum = maximums.max
    private val range = guessRange(minimum, maximum)

    /**
     * Minimum bound according to negativeness and size of byte
     */
    val minimumBound: Option[BigInt] = range.map(_.minimum)

    /**
     * Maximum bound according to negativeness and size of byte
     */
    val maximumBound: Option[BigInt] = range.map(_.maximum)
  }

  /**
   * Tries to extract all unreduced integer fields and modify minimum/maximum values
   *
   * @param original is unreduced JSON Schema with integer fields
   *                 and minimum/maximum fields as JArray in it
   * @return JSON with reduced minimum/maximum properties
   */
  def reduceIntegerFieldRange(original: JValue) = {
    val integerField: List[IntegerFieldReducer] = for { JObject(field) <- original
                                                        JField("type", JString("integer")) <- field
                                                        JField("minimum", JArray(minimum)) <- field
                                                        JField("maximum", JArray(maximum)) <- field
    } yield IntegerFieldReducer(minimum, maximum)

    integerField match {
      case head :: Nil if (head.maximumBound.isDefined && head.minimumBound.isDefined) =>
        original merge JObject("minimum" -> JInt(head.minimumBound.get),
                               "maximum" -> JInt(head.maximumBound.get))
      case _ => original
    }
  }
}
