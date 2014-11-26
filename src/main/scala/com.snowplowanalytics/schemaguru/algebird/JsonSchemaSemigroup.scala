/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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
package algebird

// Algebird
import com.twitter.algebird.Semigroup

// json4s
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.scalaz.JsonScalaz._

// This project
import generators.JsonSchemaMerger

/**
 * Being a commutative Semigroup lets us
 * reduce (i.e. merge) JSON Schemas in
 * e.g. a MapReduce environment.
 */
class JsonSchemaSemigroup extends Semigroup[JValue] {

  def plus(left: JValue, right: JValue): JValue =
    JsonSchemaMerger.merge2(left, right)

  /**
   * Override this because there is a faster
   * way to do this sum than reduceLeftOption
   * on plus.
   */
  override def sumOption(iter: TraversableOnce[JValue]): Option[JValue] =
    JsonSchemaMerger.mergeN(iter)
}
