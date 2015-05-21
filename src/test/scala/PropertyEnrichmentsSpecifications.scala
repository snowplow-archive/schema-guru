import org.specs2.scalaz.ValidationMatchers
import org.specs2.{Specification, ScalaCheck}
import org.json4s.JsonAST.{JObject, JString}

import com.snowplowanalytics.schemaguru.generators.JsonSchemaGenerator.Enrichment


class StringEnrichSpecification extends Specification with ScalaCheck with ValidationMatchers  { def is = s2"""
  Check string type enrichment
    recognize ISO date                        $recognizeIsoDate
    skip invalid date                         $doNotRecognizeIncorrectDate
    add date-time format to string            $enrichFieldWithDate
    """

  val correctDate = "2010-01-01T12:00:00+01:00"
  val incorrectDate = "2010-13-01T12:00:00+01:00"
  val StringT  = JObject(List(("type", JString("string"))))
  val StringWithDateT = JObject(List(("type", JString("string")), ("format", JString("date-time"))))

  def recognizeIsoDate =
    Enrichment.validateDateTime(correctDate) must beSuccessful

  def doNotRecognizeIncorrectDate =
    Enrichment.validateDateTime(incorrectDate) must beFailing

  def enrichFieldWithDate =
    Enrichment.enrichString(correctDate) mustEqual StringWithDateT
}

