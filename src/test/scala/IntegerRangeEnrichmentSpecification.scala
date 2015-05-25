import org.json4s.JsonAST.{JInt, JObject, JString}
import org.specs2.scalaz.ValidationMatchers
import org.specs2.{ScalaCheck, Specification}

import com.snowplowanalytics.schemaguru.generators.JsonSchemaGenerator.Enrichment

class IntegerRangeEnrichmentSpecification extends Specification with ValidationMatchers  { def is = s2"""
  Check integer range
    guess zero as Int32                        $guessZero
    guess Int16                                $guessInt16
    guess negative Int32                       $guessInt32Negative
    guess Int64                                $guessInt64
    check Int16 as Short                       $checkInt16Range
    check Int32 as Int                         $checkInt32Range
    check Int64 as Long                        $checkInt64Range
    add Int64 range to integer type            $enrichWithInt64Range
    """

  val int16Range = (-32768, 32767)
  val int32Range = (-2147483648, 2147483647)
  val int64Range = (-9223372036854775808L, 9223372036854775807L)

  val IntegerT  = JObject(List(("type", JString("integer"))))
  val IntegerTWithInt16Range = JObject(List(("type", JString("integer")), ("minimum", JInt(Short.MinValue: Int)), ("maximum", JInt(Short.MaxValue: Int))))
  val IntegerTWithInt64Range = JObject(List(("type", JString("integer")), ("minimum", JInt(Long.MinValue: BigInt)), ("maximum", JInt(Long.MaxValue: BigInt))))

  def guessZero =
    Enrichment.guessRange(0) must beSome(int16Range)

  def guessInt16 =
    Enrichment.guessRange(31000) must beSome(int16Range)

  def guessInt32Negative =
    Enrichment.guessRange(-34000) must beSome(int32Range)

  def guessInt64 =
    Enrichment.guessRange(9223372036854775806L) must beSome(int64Range)

  def checkInt16Range =
    int16Range must beEqualTo((Short.MinValue, Short.MaxValue))

  def checkInt32Range =
    int32Range must beEqualTo((Int.MinValue, Int.MaxValue))

  def checkInt64Range =
    int64Range must beEqualTo((Long.MinValue, Long.MaxValue))

  def enrichWithInt16Range =
    Enrichment.enrichInteger(3000) mustEqual IntegerTWithInt16Range

  def enrichWithInt64Range =
    Enrichment.enrichInteger(-9223372036854775806L) mustEqual IntegerTWithInt64Range

}

