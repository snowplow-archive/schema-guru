import org.specs2.scalaz.ValidationMatchers
import org.specs2.Specification
import org.json4s._
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._

import com.snowplowanalytics.schemaguru.generators.JsonSchemaMerger.mergeJsonSchemas


class MergeSpecification extends Specification with ValidationMatchers  { def is = s2"""
  Check integer merge
    maintain all types in array                            $maintainTypesInArray
    merge maximum values                                   $mergeMaximumValues
    merge minimum values                                   $mergeMinimumValues
    merge two instances                                    $mergeMinimumValuesForInt32
    merge integer with number must result in number        $mergeIntegerWithNumber
    """

  implicit val formats = DefaultFormats

  val StringT: JObject = ("type" -> "string")
  val IntegerT: JObject = ("type" -> "integer")
  val DecimalT: JObject = ("type" -> "number")
  val jObjectWithInt16: JObject = ("properties", ("test_key", IntegerT ~ ("maximum", JInt(3)) ~ ("minimum", JInt(-2))))
  val jObjectWithInt32: JObject = ("properties", ("test_key", IntegerT ~ ("maximum", JInt(3)) ~ ("minimum", JInt(-34000))))
  val jObjectWithNumber: JObject = ("properties", ("test_key", DecimalT ~ ("maximum", JDecimal(3.3)) ~ ("minimum", JInt(-34000))))

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

}
