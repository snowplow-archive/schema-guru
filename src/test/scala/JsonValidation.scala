import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.specs2.{Specification, ScalaCheck}
import org.scalacheck.Prop.all

/**
 * This test serve us as reminder that we should decide
 * whether duplicate keys are valid
 */
class JsonDuplicatedKeys extends Specification with ScalaCheck { def is = s2"""

  Decide whether duplicate keys in JSON are valid
    merged                                     $keysMerged
    """

  def keysMerged = all {
    val oneKeyJson = "{\"format\":\"date-time\"}"
    val duplicatedKeyJson = ("format", "date-time") ~ ("format", "date-time")
    compact(duplicatedKeyJson) == oneKeyJson
  }.pendingUntilFixed
}

