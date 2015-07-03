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
package utils

// json4s
import org.json4s._
import org.json4s.JsonDSL._

/**
 * Used to annotate schema with data for self-describing schema
 *
 * @param vendor vendor of schema
 * @param name name of schema (parameter isn't optional)
 * @param version optional version
 */
case class SelfDescribingSchema(vendor: String, name: Option[String], version: Option[String]) {
  val selfDescribingSchemaURI = "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#"

  def descriptSchema(schema: JValue): JValue = {
    val uri: JObject = ("$schema", selfDescribingSchemaURI)

    val selfObject: JObject = ("self",
      (("vendor", vendor): JObject) ~
      (("name", name.getOrElse("unspecified")): JObject) ~
      (("version", version.getOrElse("0-1-0")): JObject) ~
      ("format", "jsonschema"))

    uri.merge(selfObject).merge(schema)
  }
}
