package com.schema.guru

// Generators
import generators.{JsonSchemaGenerator => JSG}

// Scalaz
import scalaz._
import Scalaz._

// Jackson
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.core.JsonParseException

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.scalaz.JsonScalaz._

/**
 * Launches the process to convert JSONs into JSONSchema
 */
object Main {

  private val PathToRoot = "//vagrant//snowplow//3-enrich//scala-jsonschema-creator//src//main//scala//com.snowplowanalytics.snowplow.enrich//common//resources//"
  // System.getProperty("user.dir") == "//vagrant//snowplow//3-enrich//scala-jsonschema-creator

  def main(args: Array[String]) {

    // We can ask for the Folder Path in the CLI Args
    // val dirPath = args(0) - Will need to check that this exists before trying to grab it.

    // RUNTIME OPERATIONS //

    val returned = getJsonFromFolder(PathToRoot).toList

    val successes = for {
      Success(json) <- returned
    } yield {
      transformValueToArray(json)
    }

    val merged: JValue = mergeJson(successes)

    val schema: JValue = JSG.jsonToSchema(merged)

    println(pretty(render(schema)))
  }

  /**
   * Returns a validated List of JSONs from the folder it was
   * pointed at.
   *
   * @param dir The directory we are going to get JSONs from
   * @param ext The extension of the file we are going to be
   *        attempting to grab
   * @return an Array with validated JSONs nested inside
   */
  private def getJsonFromFolder(dir: String, ext: String = "txt"): Array[Validation[String, JValue]] =
    for {
      filePath <- new java.io.File(dir).listFiles.filter(_.getName.endsWith("." + ext))
    } yield {
      try {
        val file = scala.io.Source.fromFile(filePath)
        val content = file.mkString
        parse(content).success
      } catch {
        case e: JsonParseException => {
          val exception = e.toString
          s"File [$filePath] contents failed to parse into JSON: [$exception]".fail
        }
        case e: Exception => {
          val exception = e.toString
          s"File [$filePath] fetching and parsing failed: [$exception]".fail
        }
      }
    }

  /**
   * Merges the List of JSONs it is passed together into one 
   * cumulative JSON
   *
   * @param in The list of JSONs we need to merge together
   * @return the unified JSON 
   */
  private def mergeJson(in: List[JValue], accum: JValue = Nil): JValue = 
    in match {
      case x :: xs => mergeJson(xs, x.merge(accum))
      case Nil     => accum
    }

  /**
   * Converts all entities of the JSON into arrays so that we can merge 
   * elements together without losing data. Need to pass in a JString 
   * to each of these new arrays to differentiate between these and actual 
   * arrays.
   *
   * @param json The JSON that needs to have its values updated
   * @return the modified JSON
   */
  private def transformValueToArray(json: JValue): JValue = 
    json transformField {
      case (k, JString(v))  => (k, JArray(List(JString("type: name-value"), JString(v))))
      case (k, JInt(v))     => (k, JArray(List(JString("type: name-value"), JInt(v))))
      case (k, JDecimal(v)) => (k, JArray(List(JString("type: name-value"), JDecimal(v))))
      case (k, JDouble(v))  => (k, JArray(List(JString("type: name-value"), JDouble(v))))
      case (k, JBool(v))    => (k, JArray(List(JString("type: name-value"), JBool(v))))
      case (k, JNull)       => (k, JArray(List(JString("type: name-value"), JNull)))
    }
}
