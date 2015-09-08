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

// Spark
import org.apache.spark.rdd.RDD

// json4s
import org.json4s.JValue

// This library
import schema._
import schema.Helpers._
import generators._

/**
 * Copy of schemaguru.SchemaGuru modified for RDD
 * TODO: rewrite http://stackoverflow.com/questions/32233202/treat-spark-rdd-like-plain-seq
 */
object SchemaGuruRDD extends Serializable {
  /**
   * Takes the valid list of JSONs, converts them into micro-schemas (schemas
   * which will validate a single value)
   * Don't forget that inside ``jsonToSchema`` merge happening for
   *
   * @param jsonList The Validated JSON list
   * @param context cardinality for detecting possible enums
   * @return result result of converting instances to micro-schemas
   */
  def convertsJsonsToSchema(jsonList: RDD[ValidJson], context: SchemaContext): JsonConvertResultRDD = {

    val generator = SchemaGenerator(context)

    // TODO: find a way to do List-like partition on RDDs to avoid double traverse on jsonList and schemaList

    val validJsons: RDD[JValue] = jsonList.flatMap {
      case Success(json) => List(json)
      case _ => Nil
    }

    val failJsons: RDD[String] = jsonList.flatMap {
      case Failure(str) => List(str)
      case _ => Nil
    }

    val schemaList: RDD[ValidSchema] =
      validJsons.map(generator.jsonToSchema(_))

    val validSchemas: RDD[JsonSchema] = schemaList.flatMap {
      case Success(json) => List(json)
      case _ => Nil
    }

    val failSchemas: RDD[String] = schemaList.flatMap {
      case Failure(str) => List(str)
      case _ => Nil
    }

    JsonConvertResultRDD(validSchemas, failJsons ++ failSchemas)
  }

  /**
   * Merge all micro-schemas into one, transform it, analyze for any warnings
   * like possible duplicated keys
   *
   * @param jsonConvertResult result of converting instances to micro-schemas
   * @param schemaContext context with all information for create and merge
   * @return result of merge and transformations with Schema, errors and warnings
   */
  def mergeAndTransform(jsonConvertResult: JsonConvertResultRDD, schemaContext: SchemaContext): SchemaGuruResultRDD = {

    implicit val context = schemaContext

    val mergedSchema: JsonSchema = jsonConvertResult.schemas.reduce(_.merge(_))

    val schema = mergedSchema match {
      case complex: SchemaWithTransform[_] =>
        complex.transform { encaseNumericRange }
               .transform { correctMaxLengths }
               .transform { substituteEnums(schemaContext) }
      case _ => mergedSchema
    }

    val duplicates = LevenshteinAnnotator.getDuplicates(extractKeys(schema))

    SchemaGuruResultRDD(schema, jsonConvertResult.errors, List(PossibleDuplicatesWarning(duplicates)))
  }

}
