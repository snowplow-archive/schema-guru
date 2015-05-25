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
    """

  implicit val formats = DefaultFormats

  val StringT: JObject = ("type" -> "string")
  val IntegerT: JObject = ("type" -> "integer")
  val j: JObject = ("maximum" -> 10)

  def maintainTypesInArray = {
    val merged = mergeJsonSchemas(List(StringT, StringT, StringT, IntegerT, StringT))
    (merged \ "type").extract[List[String]] must beEqualTo(List("string", "integer"))
  }

  def mergeMaximumValues = {
    val jsonList: List[JObject] = List(("maximum" -> 2), ("maximum" -> 5), ("maximum" -> 3))
    val merged = mergeJsonSchemas(jsonList)
    (merged \ "maximum").extract[BigInt] must beEqualTo(5)
  }

  def mergeMinimumValues = {
    val jsonList: List[JObject] = List(("minimum" -> -2), ("minimum" -> -1000), ("minimum" -> 5))
    val merged = mergeJsonSchemas(jsonList)
    (merged \ "minimum").extract[BigInt] must beEqualTo(-1000)
  }
}
