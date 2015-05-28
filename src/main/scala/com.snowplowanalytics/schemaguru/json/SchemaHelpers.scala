package com.snowplowanalytics.schemaguru.json

import scala.language.implicitConversions

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

object SchemaHelpers {
  /**
   * Filters and extracts list of Ints from List of JValues
   */
  implicit def extractIntsFromJValues(jValues: List[JValue]): List[BigInt] =
    for (jValue <- jValues; JInt(int) <- jValue) yield int

  implicit def extractNumericFromJValues(jValues: List[JValue]): List[Double] = {
    val doubles = for (jValue <- jValues; JDouble(double) <- jValue) yield double
    val ints = for (jValue <- jValues; JInt(int) <- jValue) yield int.toDouble
    doubles ++ ints
  }

  implicit def extractStringsFromJValues(jValues: List[JValue]): List[String] =
    for (jValue <- jValues; JString(str) <- jValue) yield str

  implicit val formats = DefaultFormats // Brings in default date formats etc.

  /**
   * Some types in JSON Schema can be presented as product types
   * e.g. "type": ["string", "number"].
   * This predicate detects if current field contains type we're looking for
   * @param value is possible value with JArray, JString or any other JValue
   * @param string string we're looking for. Possible one of schema types
   * @return whether string were found
   */
  def contains(value: JValue, string: String) = value match {
    case JString(content) if (content == string) => true
    case JArray(types) if types.contains(JString(string)) => true
    case _ => false
  }

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
   * Holds all information about merged JSON Schema Integer
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
   * Tries to extract unreduced integer field
   * Unreduced state imply it has minimum and maximum as arrays
   *
   * @param jField any JValue, but for extraction it need to be
   *               JObject with type integer, and minimum/maximum arrays
   * @return reducer if it is really integer field
   */
  private def extractIntegerField(jField: JValue): Option[IntegerFieldReducer] =  {
    val list: List[IntegerFieldReducer] = for {
      JObject(field) <- jField
      JField("minimum", JArray(minimum)) <- field
      JField("maximum", JArray(maximum)) <- field
      JField("type", types) <- field
      if (contains(types, "integer"))
    } yield IntegerFieldReducer(minimum, maximum)
    list.headOption
  }

  /**
   * Tries to extract all unreduced integer fields and modify minimum/maximum values
   *
   * @param original is unreduced JSON Schema with integer field
   *                 and minimum/maximum fields as JArray in it
   * @return JSON with reduced minimum/maximum properties
   */
  def reduceIntegerFieldRange(original: JValue) = {
    val integerField = extractIntegerField(original)
    integerField match {
      case Some(reducer) => original merge JObject("minimum" -> JInt(reducer.minimumBound),
                                                   "maximum" -> JInt(reducer.maximumBound))
      case None => original
    }
  }

  /**
   * Holds information about merged JSON Schema Number
   *
   * @param minimums list of all encountered maximum values
   */
  private case class NumberFieldReducer(minimums: List[Double]) {
    def isNegative = minimums.min < 0
  }

  /**
   * Tries to extract unreduced number field
   * Unreduced state imply it has minimum field as array
   *
   * @param jField any JValue, but for extraction it need to be
   *               JObject with type number, and minimum array
   * @return reducer if it is really number field
   */
  private def extractNumberField(jField: JValue): Option[NumberFieldReducer] =  {
    val list: List[NumberFieldReducer] = for {
      JObject(field) <- jField
      JField("minimum", JArray(minimum)) <- field
      JField("type", types) <- field
      if (contains(types, "number"))
    } yield NumberFieldReducer(minimum)
    list.headOption
  }

  /**
   * Eliminates maximum property possible left by merge with integer
   * and also minimum if number could be negative (otherwise set it to 0)
   *
   * @param original is unreduced JSON Schema with number field
   *                 and minimum field as JArray in it
   */
  def reduceNumberFieldRange(original: JValue) = {
    val numberField = extractNumberField(original)
    numberField match {
      case Some(reducer) => original.merge(JObject("minimum" -> JInt(0)))  // it may be removed further
                                    .removeField { case JField("minimum", _) => reducer.isNegative
                                                   case JField("maximum", _) => true
                                                   case _ => false }
      case None => original
    }
  }

  /**
   * Check if field type contains both integer and number
   *
   * @param jArray value of type
   */
  def isMergedNumber(jArray: List[JValue]) =
    jArray.sorted(JValueOrdering.toScalaOrdering) == List(JString("integer"), JString("number"))

  /**
   * Holds information about merged JSON Schema String
   *
   * @param formats list of all encountered formats
   */
  private case class StringFieldReducer(formats: List[String]) {
    def getFormat: String = {
      val formatSet = formats.toSet.toList
      if (formatSet.size == 1) formatSet.head
      else "none"
    }
  }

  /**
   * Tries to extract unreduced string field
   * Unreduced state imply it has format field as array
   *
   * @param jField any JValue, but for extraction it need to be
   *               JObject with type string, and format array
   * @return reducer if it is really string field
   */
  private def extractStringField(jField: JValue): Option[StringFieldReducer] =  {
    val list: List[StringFieldReducer] = for {
      JObject(field) <- jField
      JField("format", JArray(formats)) <- field
      JField("type", types) <- field
      if (contains(types, "string"))
    } yield StringFieldReducer(formats)
    list.headOption
  }

  /**
   * Eliminates format property if more than one format presented
   *
   * @param original is unreduced JSON Schema with string field
   *                 and format property as JArray in it
   */
  def reduceStringFieldFormat(original: JValue) = {
    val stringField = extractStringField(original)
    stringField match {
      case Some(reducer) => original.merge(JObject("format" -> JString(reducer.getFormat)))  // it may be removed further
                                    .removeField { case JField("format", JString("none")) => true
                                                   case _ => false }
      case None => original
    }
  }
}
