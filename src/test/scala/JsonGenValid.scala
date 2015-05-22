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

  def arbitraryIsoDate: Arbitrary[String] =
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

  def keys(n: Int): Gen[List[String]] =
    Gen.listOfN(n, Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString))

  def values(n: Int, depth: Int) =
    Gen.listOfN(n, value(depth))

  def value(depth: Int): Gen[JValue] =
    if (depth == 0)
      terminalType
    else
      Gen.oneOf(jsonType(depth - 1), terminalType)

  def terminalType: Gen[JValue] = Gen.oneOf(
    arbitraryJString,
    arbitraryJInt,
    arbitraryJBool,
    arbitraryJUuid,
    arbitraryJIsoDate
  )

  def arbitraryJString =
    Gen.listOf(Gen.alphaChar).map(_.mkString).suchThat(_.forall(_.isLetter)).map(JString(_))

  def arbitraryJInt =
    Arbitrary.arbitrary[Int].map(JInt(_))

  def arbitraryJBool =
    Arbitrary.arbitrary[Boolean].map(JBool(_))

  def arbitraryJUuid =
    Gen.uuid.map(x => JString(x.toString))

  def arbitraryJIsoDate =
    Arbitrary.arbitrary(arbitraryIsoDate).map(JString(_))

  /**
   * Generates JSON with predefined keys and specified types
   * 
   * @param typeMap map of key to arbitrary JValues
   * @return arbitrary JSON with predefined keys and arbitrary values
   */
  def generateJsonWithKeys(typeMap: Map[String, Gen[JValue]]): Gen[JObject] = {
    val mapWithGeneratedValues = for { (k: String, v: Gen[JValue]) <- typeMap; vl <- v.sample} yield (k, vl)
    val jObject = JObject(mapWithGeneratedValues.toList)
    Gen.const(jObject)
  }
}
