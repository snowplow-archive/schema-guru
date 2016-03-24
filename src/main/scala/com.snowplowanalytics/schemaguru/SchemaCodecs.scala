/*
 * Copyright (c) 2012-2016 Snowplow Analytics Ltd. All rights reserved.
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
import org.json4s._
import org.json4s.jackson.Serialization

// This library
import Common.{ Schema, SchemaDescription, SchemaVer }

/**
 * Module containing json4s decoders/encoders for standard Schema/Iglu datatypes
 */
object SchemaCodecs {

  private implicit val defaulFormats: Formats = Serialization.formats(NoTypeHints) + SchemaVerSerializer

  // Public formats. Import it
  lazy val formats: Formats = defaulFormats + SchemaSerializer

  /**
   * Extract SchemaVer (*-*-*) from JValue
   */
  object SchemaVerSerializer extends CustomSerializer[SchemaVer](_ => (
    {
      case JString(version) => SchemaVer.parse(version) match {
        case Some(schemaVer) => schemaVer
        case None => throw new MappingException("Can't convert " + version + " to SchemaVer")
      }
      case x => throw new MappingException("Can't convert " + x + " to SchemaVer")
    },
    {
      case x: SchemaVer => JString(s"${x.model}-${x.revision}-${x.addition}")
    }
    ))

  /**
   * Extract Schema with self-description and data out of JValue
   */
  object SchemaSerializer extends CustomSerializer[Schema](_ => (
    {
      case fullSchema: JObject =>
        (fullSchema \ "self").extractOpt[SchemaDescription] match {
          case Some(desc) =>
            val cleanSchema = fullSchema.obj.filterNot {
              case ("self", _) => true
              case _ => false
            }
            Schema(desc, JObject(cleanSchema))
          case None => throw new MappingException("JSON isn't self-describing")
        }
      case _ => throw new MappingException("Not an JSON object")
    },
    {
      case x: Schema => JObject(JField("self", Extraction.decompose(x.self)) :: x.data.obj)
    }
    ))
}
