import org.specs2.scalaz.ValidationMatchers
import org.specs2.{Specification, ScalaCheck}
import org.json4s.JsonAST.{JObject, JString}

import com.snowplowanalytics.schemaguru.generators.JsonSchemaGenerator.Enrichment


class StringEnrichSpecification extends Specification with ScalaCheck with ValidationMatchers  { def is = s2"""
  Check string type enrichment
    recognize UUID                            $recognizeUuid
    recognize ISO date                        $recognizeIsoDate
    skip invalid date                         $doNotRecognizeIncorrectDate
    skip invalid date as number               $doNotRecognizeIncorrectDateAsNum
    add date-time format to string            $enrichFieldWithDate
    add ipv4 format to string                 $enrichFieldWithIp4
    """

  val correctUuid = "f0e89550-7fda-11e4-bbe8-22000ad9bf74"
  val correctDate = "2010-01-01T12:00:00+01:00"
  val incorrectDate = "2010-13-01T12:00:00+01:00"
  val incorrectDateAsNumString = "23"
  val correctIp = "192.1.1.2"
  val StringT  = JObject(List(("type", JString("string"))))
  val StringWithDateT = JObject(List(("type", JString("string")), ("format", JString("date-time"))))
  val StringWitIp4 = JObject(List(("type", JString("string")), ("format", JString("ipv4"))))

  def recognizeUuid =
    Enrichment.suggestUuidFormat(correctUuid) must beSome("uuid")

  def recognizeIsoDate =
    Enrichment.suggestTimeFormat(correctDate) must beSome("date-time")

  def doNotRecognizeIncorrectDate =
    Enrichment.suggestTimeFormat(incorrectDate) must beNone

  def doNotRecognizeIncorrectDateAsNum =
    Enrichment.suggestTimeFormat(incorrectDateAsNumString) must beNone

  def enrichFieldWithDate =
    Enrichment.enrichString(correctDate) mustEqual StringWithDateT

  def enrichFieldWithIp4 =
    Enrichment.enrichString(correctIp) mustEqual StringWitIp4
}

