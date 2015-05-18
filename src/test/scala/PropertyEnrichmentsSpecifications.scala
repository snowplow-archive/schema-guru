import org.json4s.JsonAST.{JString, JObject}
import org.scalacheck.Properties
import org.scalacheck.Prop.{forAll, all}

import com.snowplowanalytics.schemaguru.generators.JsonSchemaGenerator.Enrichment

object StringSpecification extends Properties("String") {

  val correctDate = "2010-01-01T12:00:00+01:00"
  val incorrectDate = "2010-13-01T12:00:00+01:00"

  val StringT  = JObject(List(("type", JString("string"))))
  val StringWithDateT = JObject(List(("type", JString("string")), ("format", JString("date-time"))))

  property("recognizeIsoDate") = all {
    Enrichment.validateDateTime(correctDate).isSuccess
  }

  property("notRecognizeIncorrectDate") = all {
    Enrichment.validateDateTime(incorrectDate).isFailure
  }

  property("enrichFieldWithDate") = all {
    Enrichment.enrichString(correctDate) == StringWithDateT
  }
}