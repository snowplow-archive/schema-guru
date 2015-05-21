import scala.util.parsing.json._

import org.joda.time.DateTime
import org.json4s.JsonAST.JObject
import scalaz._, Scalaz._

import org.scalacheck._
import org.specs2.scalaz.ValidationMatchers
import org.specs2.{Specification, ScalaCheck}

import com.snowplowanalytics.schemaguru.SchemaGuru.convertsJsonsToSchema
import com.snowplowanalytics.schemaguru.ValidJsonList


class SelfValidSpe extends Specification with ScalaCheck with ValidationMatchers with JsonGen { def is = s2"""
  Derive schema from random generated JSON and validate against itself
  """

  def generateObject = prop { (event: JObject) =>
    val validJson: ValidJsonList = List(event.success)
    val schema = convertsJsonsToSchema(validJson)

  }.pendingUntilFixed
}

/**
 * Trait responsible for arbitrary JSON
 */
trait JsonGen {
  /**
   * Generate String of valid ISO-8601 date
   */
  def arbitaryIsoDate: Arbitrary[String] =
    Arbitrary(Gen.choose(0L, 1922659200L * 1000).map(new DateTime(_).toString))

  /**
   * Generate either a JSONArray or a JSONObject
   */
  def jsonType(depth: Int): Gen[JSONType] = Gen.oneOf(jsonArray(depth), jsonObject(depth))

  /**
   * Generate a JSONArray
   */
  def jsonArray(depth: Int): Gen[JSONArray] = for {
    n    <- Gen.choose(1, 4)        // length of array
    vals <- values(n, depth)
  } yield JSONArray(vals)

  /**
   * Generate a JSONObject
   */
  def jsonObject(depth: Int): Gen[JSONObject] = for {
    n    <- Gen.choose(1, 3)
    ks   <- keys(n)
    vals <- values(n, depth)
  } yield JSONObject(Map((ks zip vals):_*))

  /**
   * Generate a list of keys to be used in the map of a JSONObject
   */
  def keys(n: Int) = Gen.listOfN(n, Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString))

  /**
   * Generate a list of values to be used in the map of a JSONObject or in the list
   * of a JSONArray.
   */
  def values(n: Int, depth: Int) = Gen.listOfN(n, value(depth))

  /**
   * Generate a value to be used in the map of a JSONObject or in the list
   * of a JSONArray.
   */
  def value(depth: Int) =
    if (depth == 0)
      terminalType
    else
      Gen.oneOf(jsonType(depth - 1), terminalType)

  /**
   * Generate a terminal non-object value.
   * One of numeric, alphaNumeric, string, etc
   */
  def terminalType = {
    Gen.oneOf(
      Gen.listOf(Gen.alphaNumChar).map(_.mkString),
      Arbitrary.arbitrary[Int],
      Arbitrary.arbitrary(arbitaryIsoDate),
      Arbitrary.arbitrary[Boolean]
    )
  }
}

