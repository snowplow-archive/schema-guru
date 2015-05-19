import org.specs2.{Specification, ScalaCheck}
import org.json4s.JsonAST.{JObject, JString}
import org.scalacheck.Prop.all

import com.snowplowanalytics.schemaguru.generators.JsonSchemaGenerator.Enrichment


class StringEnrichSpecification extends Specification with ScalaCheck  { def is = s2"""
  Check string type enrichment
    recognize ISO date                        $recognizeIsoDate
    skip invalid date                         $doNotRecognizeIncorrectDate
    add date-time format to string            $enrichFieldWithDate
    """

  val correctDate = "2010-01-01T12:00:00+01:00"
  val incorrectDate = "2010-13-01T12:00:00+01:00"
  val StringT  = JObject(List(("type", JString("string"))))
  val StringWithDateT = JObject(List(("type", JString("string")), ("format", JString("date-time"))))


  def recognizeIsoDate = all {
    Enrichment.validateDateTime(correctDate).isSuccess
  }

  def doNotRecognizeIncorrectDate = all {
    Enrichment.validateDateTime(incorrectDate).isFailure
  }

  def enrichFieldWithDate = all {
    Enrichment.enrichString(correctDate) == StringWithDateT
  }
}
