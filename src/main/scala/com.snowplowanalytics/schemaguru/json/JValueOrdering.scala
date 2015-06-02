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
package json

// Scalaz
import scalaz._
import Scalaz._

// json4s
import org.json4s._

/**
 * Offer some of JValue instances compare with
 * instances of same type
 */
object JValueOrdering extends Order[JValue] {
  def order(x: JValue, y: JValue): Ordering = (x, y) match {
    case (JInt(a), JInt(b))         => a ?|? b
    case (JString(a), JString(b))   => a ?|? b
    case (JDouble(a), JDouble(b))   => a ?|? b
    case (JDecimal(a), JDecimal(b)) => a ?|? b
    case (JArray(a), JArray(b))     => a.length ?|? b.length
    case _                          => Ordering.EQ
  }
}
