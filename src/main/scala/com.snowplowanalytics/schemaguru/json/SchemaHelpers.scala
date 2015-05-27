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
    ranges.find(r => r.minimum <= min && r.maximum >= max).get

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
    val minimumBound: BigInt = range.minimum

    /**
     * Maximum bound according to negativeness and size of byte
     */
    val maximumBound: BigInt = range.maximum
  }

  /**
   * Tries to extract all unreduced integer fields and modify minimum/maximum values
   *
   * @param original is unreduced JSON Schema with integer field
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
      case head :: Nil => original merge JObject("minimum" -> JInt(head.minimumBound),
                                                 "maximum" -> JInt(head.maximumBound))
      case _ => original
    }
  }

  private case object NumberField

  /**
   * Eliminates minimum and maximum properties possible left by merge with integer
   */
  def eliminateMinMaxForNumber(original: JValue) = {
    val numberField: List[NumberField.type] = for { JObject(field) <- original
                                                    JField("type", JString("number")) <- field
                                                    JField("minimum", JArray(_)) <- field
                                                    JField("maximum", JArray(_)) <- field
    } yield NumberField

    numberField match {
      case head :: Nil => original removeField { case JField("maximum", _) => true
                                                 case JField("minimum", _) => true
                                                 case _ => false }
      case _ => original
    }
  }

  /**
   * Check if field type contains both integer and number
   *
   * @param jArray value of type
   */
  def isMergedNumber(jArray: List[JValue]) =
    jArray.sorted(JValueOrdering.toScalaOrdering) == List(JString("integer"), JString("number"))

}
