import org.json4s.jackson.JsonMethods._
import org.json4s.JsonAST.{JInt, JObject, JString}
import org.specs2.Specification

import com.snowplowanalytics.schemaguru.json.SchemaHelpers

class IntegerRangeEnrichmentSpecification extends Specification { def is = s2"""
  Check integer range
    guess zero as positive                     $guessZero
    guess Int16                                $guessInt16
    guess negative Int32                       $guessInt32Negative
    guess Int64                                $guessInt64
    check Int16 as Short                       $checkInt16Range
    check Int32 as Int                         $checkInt32Range
    check Int64 as Long                        $checkInt64Range
    """

  case class Range(minimum: BigInt, maximum: BigInt)

  val int16Range = Range(-32768, 32767)
  val int32Range = Range(-2147483648, 2147483647)
  val int64Range = Range(-9223372036854775808L, 9223372036854775807L)

  val IntegerT  = JObject(List(("type", JString("integer"))))
  val IntegerTWithInt16Range = JObject(List(("type", JString("integer")), ("minimum", JInt(Short.MinValue: Int)), ("maximum", JInt(Short.MaxValue: Int))))
  val IntegerTWithInt64Range = JObject(List(("type", JString("integer")), ("minimum", JInt(Long.MinValue: BigInt)), ("maximum", JInt(Long.MaxValue: BigInt))))

  val schemaWithPositiveInt16 = parse("""{"type": "integer", "minimum": [21, 100, 0, 31], "maximum": [30000, 16000, 100]}""")

  def guessZero =
    SchemaHelpers.IntegerFieldReducer(List(0), List(0)).minimumBound must beEqualTo(0)

  def guessInt16 =
    SchemaHelpers.IntegerFieldReducer(List(-1), List(31000)).minimumBound must beEqualTo(int16Range.minimum)

  def guessInt32Negative =
    SchemaHelpers.IntegerFieldReducer(List(-34000), List(3000)).minimumBound must beEqualTo(int32Range.minimum)

  def guessInt64 =
    SchemaHelpers.IntegerFieldReducer(List(-34000), List(9223372036854775806L)).minimumBound must beEqualTo(int64Range.minimum)

  def checkInt16Range =
    int16Range must beEqualTo(Range(Short.MinValue.toInt, Short.MaxValue.toInt))

  def checkInt32Range =
    int32Range must beEqualTo(Range(Int.MinValue, Int.MaxValue))

  def checkInt64Range =
    int64Range must beEqualTo(Range(Long.MinValue, Long.MaxValue))

  def enrichWithInt16Range =
    SchemaHelpers.reduceIntegerFieldRange(schemaWithPositiveInt16) mustEqual IntegerTWithInt16Range

}

