import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.specs2.Specification


/**
 * This test serve us as reminder that we should decide
 * whether duplicate keys are valid
 */
class JsonDuplicatedKeys extends Specification  { def is = s2"""

  Decide whether duplicate keys in JSON are valid
    merged                                     $keysMerged
    """

  def keysMerged = {
    val oneKeyJson = "{\"format\":\"date-time\"}"
    val duplicatedKeyJson = ("format", "date-time") ~ ("format", "date-time")
    compact(duplicatedKeyJson) must beEqualTo(oneKeyJson)
  }.pendingUntilFixed
}

