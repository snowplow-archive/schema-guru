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

// specs2
import org.specs2.Specification
import org.specs2.matcher.JsonMatchers

import scala.concurrent.duration._

// spray
import spray.http._
import spray.testkit.Specs2RouteTest

class ProcessSpec extends Specification with Specs2RouteTest with JsonMatchers with SchemaGuruRoutes { def is = s2"""
  Check API endpoint
    process request with one JSON       $processRequestWithJson
  """

  implicit def actorRefFactory = system
  val entryPointUrl = "/upload"

  implicit val routeTestTimeout = RouteTestTimeout((4 * 1000 * 1000).microsecond)

  def processRequestWithJson = {
    val json = """{"referrer":"127.0.0.1", "id":42 }"""
    val payload = MultipartFormData(
      Seq(BodyPart(HttpEntity(MediaTypes.`multipart/form-data`, json), "test.json"))
    )

    Post(entryPointUrl, payload) ~> rootRoute ~> check {
      body.data.asString must /("schema") /("type" -> "object")
    }
  }
}