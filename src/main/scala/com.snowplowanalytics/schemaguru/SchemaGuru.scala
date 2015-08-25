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

// Scalaz
import scalaz._
import Scalaz._

// This library
import generators.{
  SchemaGenerator,
  LevenshteinAnnotator
}
import schema.Helpers._
import schema.SchemaWithTransform

object SchemaGuru {
  /**
   * Takes the valid list of JSONs, converts them into micro-schemas (schemas
   * which will validate a single value)
   * Don't forget that inside ``jsonToSchema`` merge happening for
   *
   * @param jsonList The Validated JSON list
   * @param context cardinality for detecting possible enums
   * @return result result of converting instances to micro-schemas
   */
  def convertsJsonsToSchema(jsonList: ValidJsonList, context: SchemaContext): JsonConvertResult = {

    val generator = SchemaGenerator(context)

    // TODO: do not iterate lists each time

    // Process all valid JSONs into Schemas
    val validatedSchemas = for {
      Success(json) <- jsonList
    } yield {
      generator.jsonToSchema(json)
    }

    // Get all valid Schemas (for objects and arrays)
    val schemas = for {
      Success(schema) <- validatedSchemas
    } yield {
      schema
    }

    // Get errors for all non-JSONs
    val invalidJsons = for {
      Failure(err) <- jsonList
    } yield err

    // Get errors for all unacceptable Schemas (non-objects and non-arrays)
    val unacceptableJsons = for {
      Failure(err) <- validatedSchemas
    } yield err

    JsonConvertResult(schemas, invalidJsons ++ unacceptableJsons)
  }

  /**
   * Merge all micro-schemas into one, transform it, analyze for any warnings
   * like possible duplicated keys
   *
   * @param jsonConvertResult result of converting instances to micro-schemas
   * @param schemaContext context with all information for create and merge
   * @return result of merge and transformations with Schema, errors and warnings
   */
  def mergeAndTransform(jsonConvertResult: JsonConvertResult, schemaContext: SchemaContext): SchemaGuruResult = {

    implicit val monoid = getMonoid(schemaContext)

    val mergedSchema = jsonConvertResult.schemas.suml

    val schema = mergedSchema match {
      case complex: SchemaWithTransform[_] =>
        complex.transform { encaseNumericRange }
               .transform { substituteEnums(schemaContext) }
      case _ => mergedSchema
    }

    val duplicates = LevenshteinAnnotator.getDuplicates(extractKeys(schema))

    SchemaGuruResult(schema, jsonConvertResult.errors, Some(PossibleDuplicatesWarning(duplicates)))
  }
}
