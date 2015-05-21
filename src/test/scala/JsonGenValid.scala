import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import org.joda.time.DateTime
import org.json4s._
import org.json4s.jackson.JsonMethods._

import org.scalacheck._
import org.specs2.scalaz.ValidationMatchers
import org.specs2.{Specification, ScalaCheck}

import com.snowplowanalytics.schemaguru.generators.JsonSchemaGenerator.jsonToSchema


class SelfValidSpecification extends Specification with ScalaCheck with ValidationMatchers with JsonGen { def is = s2"""
  Derive schema from random generated JSON and validate against itself
    validate random JSON against derived schema            $validateJsonAgainstDerivedSchema
  """

  def validateJsonAgainstDerivedSchema = prop { (json: JValue) =>
    val factory: JsonSchemaFactory  = JsonSchemaFactory.byDefault()
    val derivedSchema = asJsonNode(jsonToSchema(json))
    val schemaSchema: JsonSchema = factory.getJsonSchema(derivedSchema)

    schemaSchema.validate(asJsonNode(json)).isSuccess must beTrue
  }.set(maxSize = 20)
}

/**
 * Trait responsible for arbitrary JSON
 */
trait JsonGen {
  implicit def arbitraryJsonType: Arbitrary[JValue] =
    Arbitrary { Gen.sized(depth => jsonType(depth)) }

  def arbitaryIsoDate: Arbitrary[String] =
    Arbitrary(Gen.choose(0L, 1922659200L * 1000).map(new DateTime(_).toString))

  def jsonType(depth: Int): Gen[JValue] =
    Gen.oneOf(jsonArray(depth), jsonObject(depth))

  def jsonArray(depth: Int): Gen[JArray] = for {
    n    <- Gen.choose(1, 4)
    vals <- values(n, depth)
  } yield JArray(vals)

  def jsonObject(depth: Int): Gen[JObject] = for {
    n <- Gen.choose(1, 4)
    ks <- keys(n)
    vals <- values(n, depth)
  } yield JObject((ks zip vals):_*)

  def keys(n: Int) =
    Gen.listOfN(n, Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString))

  def values(n: Int, depth: Int) =
    Gen.listOfN(n, value(depth))

  def value(depth: Int): Gen[JValue] =
    if (depth == 0)
      terminalType
    else
      Gen.oneOf(jsonType(depth - 1), terminalType)

  def terminalType: Gen[JValue] = Gen.oneOf(
    Gen.listOf(Gen.alphaChar).map(_.mkString).suchThat(_.forall(_.isLetter)).map(JString(_)),
    Gen.uuid.map(x => JString(x.toString)),
    Arbitrary.arbitrary(arbitaryIsoDate).map(JString(_)),
    Arbitrary.arbitrary[Int].map(JInt(_)),
    Arbitrary.arbitrary[Boolean].map(JBool(_))
  )
}
