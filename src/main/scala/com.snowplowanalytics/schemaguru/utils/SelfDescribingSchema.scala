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
package com.snowplowanalytics.schemaguru.utils

// json4s
import org.json4s._
import org.json4s.JsonDSL._

case class SelfDescribingSchema(vendor: String, name: String, version: Option[String]) {
  val selfDescribingSchemaURI = "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#"

  def descriptSchema(schema: JValue): JValue = {
    val uri: JObject = ("$schema", selfDescribingSchemaURI)

    val selfObject: JObject = ("self",
      ("vendor", vendor) ~
      ("name", name) ~
      ("version", version.getOrElse("0-1-0")) ~
      ("format", "jsonschema"))

    uri.merge(selfObject).merge(schema)
  }
}
