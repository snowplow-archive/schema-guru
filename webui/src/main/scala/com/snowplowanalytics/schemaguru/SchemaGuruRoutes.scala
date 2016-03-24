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
package webui

// Spray
import spray.http._
import spray.http.MediaTypes._
import spray.routing.HttpService

// json
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

// This library
import schema.Helpers.SchemaContext
import Common.splitValidations

trait SchemaGuruRoutes extends HttpService with HttpJsonGetters with HttpOptionsGetter {
  /**
   * Pipe route to ``convertsToJsonToSchema`` core function
   * Accept POST request with JSON files
   */
  def upload = path("upload") {
    post {
      respondWithMediaType(`application/json`) {
        entity(as[MultipartFormData]) { formData =>
          respondWithHeader(HttpHeaders.`Access-Control-Allow-Origin`(AllOrigins)) {
            detach() {
              complete {
                val (parseErrors, jsons) = splitValidations(getJsonFromRequest(formData))
                val context = SchemaContext(getCardinality(formData), quantity = Some(formData.fields.length))
                val convertResult = SchemaGuru.convertsJsonsToSchema(jsons, context)
                val mergeResult = SchemaGuru.mergeAndTransform(convertResult, context)
                val errors = getErrorsAsJson(mergeResult.errors ++ parseErrors)
                compact(
                  (("status", "processed"): JObject) ~
                  ("schema", mergeResult.schema.toJson) ~
                  ("errors", errors) ~
                  ("warning", mergeResult.warning.map(_.jsonMessage))
                )
              }
            }
          }
        }
      }
    }
  }

  // Serve static
  def index = path("") {
    getFromResource("web/index.html")
  }
  def staticJs = pathPrefix("dist") {
    compressResponse() {
      getFromResourceDirectory("web/dist")
    }
  }
  def staticCss = pathPrefix("css") {
    getFromResourceDirectory("web/css")
  }

  def rootRoute = index ~ staticJs ~ staticCss ~ upload
}
