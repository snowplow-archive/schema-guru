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

// spray
import spray.http.StatusCodes
import spray.testkit.Specs2RouteTest

class ServeSpec extends Specification with Specs2RouteTest with SchemaGuruRoutes { def is = s2"""
  serve index.html          $checkRootPresence
  serve JS                  $checkJsPresence
  serve concatenated JS     $checkJsSize
  serve compressed JS       $checkJsCompression
  do not serve unknown URL  $checkUnservedAbsence
  """

  implicit def actorRefFactory = system
  val jsUrl = "/dist/bundle.js"

  def checkRootPresence = {
    Get("/") ~> rootRoute ~> check {
      status mustEqual StatusCodes.OK
    }
  }

  def checkJsPresence = {
    Get(jsUrl) ~> rootRoute ~> check {
      status mustEqual StatusCodes.OK
    }
  }

  def checkJsSize = {
    Get(jsUrl) ~> rootRoute ~> check {
      // it should be > 85000
      body.data.length must be_>[Long](35000)
    }
  }

  def checkJsCompression = {
    Get(jsUrl) ~> rootRoute ~> check {
      header("Content-Encoding").map(_.value) must beSome("gzip")
    }
  }

  def checkUnservedAbsence = {
    Get("/nothingHere") ~> rootRoute ~> check {
      handled must beFalse
    }
  }
}
