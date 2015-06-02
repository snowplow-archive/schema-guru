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

// json4s
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

// Testing
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

