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
    add double format to string               $enrichFieldWithDouble
    add true format to string                 $enrichFieldWithBooleanT
    add false format to string                $enrichFieldWithBooleanF
    add short format to string                $enrichFieldWithShort
    add int format to string                  $enrichFieldWithInt
    add long format to string                 $enrichFieldWithLong
    add default format to string              $enrichFieldWithDefault
    recognize true boolean                    $enrichFieldWithBooleanTrue
    recognize false boolean                   $enrichFieldWithBooleanFalse
    skip invalid boolean                      $enrichFieldWithBoolean
    recognize short min                       $enrichFieldWithShortMin
    recognize short max                       $enrichFieldWithShortMax
    recognize int min                         $enrichFieldWithIntMin
    recognize int max                         $enrichFieldWithIntMax
    recognize long min                        $enrichFieldWithLongMin
    recognize long max                        $enrichFieldWithLongMax
    recognize double min                      $enrichFieldWithDoubleMin
    recognize double max                      $enrichFieldWithDoubleMax
    """

  val correctUuid = "f0e89550-7fda-11e4-bbe8-22000ad9bf74"
  val correctDate = "2010-01-01T12:00:00+01:00"
  val incorrectDate = "2010-13-01T12:00:00+01:00"
  val incorrectDateAsNumString = "23"
  val correctIp = "192.1.1.2"
  val correctTrue = "true"
  val correctFalse = "false"
  val incorrectBoolean = "notBoolean"
  val StringT  = JObject(List(("type", JString("string"))))
  val StringWithDateT = JObject(List(("type", JString("string")), ("format", JString("date-time"))))
  val StringWitIp4 = JObject(List(("type", JString("string")), ("format", JString("ipv4"))))
  val BooleanT = JObject(List(("type",JString("boolean"))))
  val NumberWithDouble = JObject(List(("type",JString("number")), ("format",JString("double"))))
  val NumberWithShort = JObject(List(("type",JString("integer")), ("format",JString("short"))))
  val NumberWithInt = JObject(List(("type",JString("integer")), ("format",JString("int"))))
  val NumberWithLong = JObject(List(("type",JString("integer")), ("format",JString("long"))))

  val StringDefault = JObject(List(("type",JString("string"))))


  def recognizeUuid =
    Enrichment.suggestUuidFormat(correctUuid) must beSome("uuid")

  def recognizeIsoDate =
    Enrichment.suggestTimeFormat(correctDate) must beSome("date-time")

  def enrichFieldWithBooleanTrue =
    Enrichment.suggestBoolean(correctTrue) must beSome("boolean")

  def enrichFieldWithBooleanFalse =
    Enrichment.suggestBoolean(correctFalse) must beSome("boolean")

  def enrichFieldWithBoolean =
    Enrichment.suggestBoolean(incorrectBoolean) must beNone

  def enrichFieldWithShortMin =
    Enrichment.suggestInteger(Short.MinValue.toString) must beSome("short")
  def enrichFieldWithShortMax =
    Enrichment.suggestInteger(Short.MaxValue.toString) must beSome("short")

  def enrichFieldWithIntMin =
    Enrichment.suggestInteger(Int.MinValue.toString) must beSome("int")
  def enrichFieldWithIntMax =
    Enrichment.suggestInteger(Int.MaxValue.toString) must beSome("int")

  def enrichFieldWithLongMin =
    Enrichment.suggestInteger(Long.MinValue.toString) must beSome("long")
  def enrichFieldWithLongMax =
    Enrichment.suggestInteger(Long.MaxValue.toString) must beSome("long")

  def enrichFieldWithDoubleMin =
    Enrichment.suggestDecimal(Double.MinValue.toString) must beSome("double")
  def enrichFieldWithDoubleMax =
    Enrichment.suggestDecimal(Double.MaxValue.toString) must beSome("double")

  def doNotRecognizeIncorrectDate =
    Enrichment.suggestTimeFormat(incorrectDate) must beNone

  def doNotRecognizeIncorrectDateAsNum =
    Enrichment.suggestTimeFormat(incorrectDateAsNumString) must beNone

  def enrichFieldWithDate =
    Enrichment.enrichString(correctDate) mustEqual StringWithDateT

  def enrichFieldWithIp4 =
    Enrichment.enrichString(correctIp) mustEqual StringWitIp4

  def enrichFieldWithBooleanT =
    Enrichment.enrichString(true.toString) mustEqual BooleanT

  def enrichFieldWithBooleanF =
    Enrichment.enrichString(false.toString) mustEqual BooleanT

  def enrichFieldWithDouble =
    Enrichment.enrichString(Double.MaxValue.toString) mustEqual NumberWithDouble

  def enrichFieldWithShort =
    Enrichment.enrichString(Short.MaxValue.toString) mustEqual NumberWithShort

  def enrichFieldWithInt =
    Enrichment.enrichString(Int.MaxValue.toString) mustEqual NumberWithInt

  def enrichFieldWithLong =
    Enrichment.enrichString(Long.MaxValue.toString) mustEqual NumberWithLong

  def enrichFieldWithDefault=
    Enrichment.enrichString("plain 'ol string") mustEqual StringDefault

}

