import org.specs2.Specification
import org.json4s._
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._

import com.snowplowanalytics.schemaguru.generators.JsonSchemaMerger.mergeJsonSchemas


class MergeSpecification extends Specification { def is = s2"""
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
  val jObjectWithoutFormat: JObject = ("properties", ("test_key", StringT ~ ("format", JString("none"))))

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
    (merged \ "properties" \ "test_key" \ "type").extract[String] must beEqualTo("number")
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
