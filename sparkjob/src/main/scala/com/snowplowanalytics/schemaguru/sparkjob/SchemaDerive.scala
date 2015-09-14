/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.schemaguru
package sparkjob

// Scalaz
import scalaz._
import Scalaz._

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.fasterxml.jackson.core.JsonParseException

// Spark
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.HashPartitioner
import org.apache.spark.rdd.RDD


// This library
import schema.Helpers.SchemaContext

object SchemaDerive {
  private val AppName = "SchemaDeriveJob"
  private val DefaultErrorsFile = "errors.log"

  /**
   * Get spark context, parse given options and run processing
   */
  def execute(master: Option[String], args: List[String], jars: Seq[String] = Nil) {
    val sc = {
      val conf = new SparkConf().setAppName(AppName).setJars(jars)
      for (m <- master) {
        conf.setMaster(m)
      }
      new SparkContext(conf)
    }

    OptionsParserCLI.parse(args) match {
      case Success(options) => {
        val result = process(sc, options)

        // Output schemas
        val schemas = result.filter(_.schemaPath.isDefined).map(r => (r.schemaPath.get, pretty(r.schema.toJson)))
        output(options.output.get, schemas)

        // Output errors
        options.errorsPath match {
          case Some(path) => result.filter(!_.errors.isEmpty).map(_.errors.mkString("\n")).saveAsTextFile(path)
          case None       => // suppress error output if errors-path wasn't given
        }
      }
      case Failure(err) => {
        val pathIndex = args.indexOf("--errors-path") // at least try to get error path
        val path: String = args.lift(pathIndex + 1).getOrElse(DefaultErrorsFile)
        sc.parallelize(List(err)).saveAsTextFile(path)
      }
    }
  }

  /**
   * Main working function.
   * Generate micro-schemas, merge them and give RDD with corresponding amount
   * of output results
   *
   * @param sc Spark Context
   * @param options Schema Guru options passed by user
   * @return RDD with single schema/errors output if schema segmentation
   *         required, with multiple otherwise
   */
  def process(sc: SparkContext, options: SchemaGuruOptions): RDD[OutputResult] = {
    val enumSets: List[JArray] = options.getEnumSets.flatMap({ case Success(l) => List(l); case Failure(_) => Nil }).toList
    val schemaContext: SchemaContext = SchemaContext(options.enumCardinality, enumSets, deriveLength = !options.noLength)

    // Decide where and which files should be parsed
    val jsonList: RDD[ValidJson] = options.ndjson match {
      case true => getJsonsFromFolderWithNDFiles(sc, options.input)
      case false => getJsonsFromFolder(sc, options.input)
    }

    val failedEnumSets: Seq[String] = options.getEnumSets.flatMap {
      case Failure(f) => List(s"Predefined enum [$f] not found")
      case Success(_) => Nil
    }

    // Produce one-element RDD for usual processing and multiple-elements for processing with segments
    options.segmentSchema match {
      case None => {
        val convertResult = SchemaGuruRDD.convertsJsonsToSchema(jsonList, schemaContext)
        val mergeResult = SchemaGuruRDD.mergeAndTransform(convertResult, schemaContext)
        val schema = Schema(mergeResult.schema, options.selfDescribing)
        val errors = mergeResult.errors.collect.toList ++ failedEnumSets ++ mergeResult.warnings.map(_.consoleMessage)
        sc.parallelize(List(OutputResult(schema, Some("result.json"), errors, options.errorsPath)))
      }
      case Some((path, _)) => {
        val nameToJsonsMapping: RDD[(String, Iterable[ValidJson])] = JsonPathExtractorRDD.mapByPathRDD(path, jsonList)
        nameToJsonsMapping map {
          case (key, jsons) => {  // jsons are Lists nested in RDD
            val convertResult = SchemaGuru.convertsJsonsToSchema(jsons.toList, schemaContext)
            val mergeResult = SchemaGuru.mergeAndTransform(convertResult, schemaContext)
            val describingInfo = options.selfDescribing.map(_.copy(name = Some(key)))
            val fileName = key + ".json"
            val file =
              if (key == "$SchemaGuruFailed") None
              else Some(fileName)
            val schema = Schema(mergeResult.schema, describingInfo)
            val errors = mergeResult.errors ++ failedEnumSets ++ mergeResult.warning.map(_.consoleMessage)
            OutputResult(schema, file, errors, options.errorsPath)
          }
        }
      }
    }
  }

  /**
   * Output pairs of (path, content)
   *
   * @param results pairs
   */
  def output(subdir: String, results: RDD[(String, String)]): Unit =
    results.partitionBy(new HashPartitioner(4))
           .saveAsHadoopFile(subdir, classOf[String], classOf[String], classOf[RDDMultipleTextOutputFormat])

  /**
   * Get JSONs from folder file by file
   *
   * @param sc Spark Context
   * @param dir path to folder
   * @return RDD with content of each file as elements
   */
  def getJsonsFromFolder(sc: SparkContext, dir: String): RDD[ValidJson] =
    sc.wholeTextFiles(dir).map(parseJson)

  /**
   * Parse folder with newline-delimited JSONs
   *
   * @param sc Spark Context
   * @param dir path to folder
   * @return RDD with each line in each file as elements
   */
  def getJsonsFromFolderWithNDFiles(sc: SparkContext, dir: String): RDD[ValidJson] =
    sc.wholeTextFiles(dir).flatMap { case (filename, content) =>
      content.split("\n").map(c => parseJson((filename, c)))
    }

  /**
   * Try to parse JSON or return error with it's file name
   *
   * @param file tuple of filename and content
   * @return validation with JValue as success or failure message
   */
  def parseJson(file: (String, String)): ValidJson = file match {
    case (filename, content) =>
      try {
        parse(content).success
      } catch {
        case e: JsonParseException =>
          s"File [$filename] contents failed to parse into JSON: [${e.getMessage}}]".failure
        case e: Exception =>
          s"File [$filename] fetching and parsing failed: [${e.getMessage}}]".failure
      }
  }
}