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
package generators

// json4s
import org.json4s._

// This library
import com.snowplowanalytics.schemaguru.json.SchemaHelper

/**
 * Base tree-like type for all JSON Schema types
 */
sealed trait SchemaType {
  /**
   * Predicate function deciding whether this object can contain children objects
   * Primitive types in schema (non-objects) are considered as leafs
   * Object are considered as branches
   * @return boolean whether object is a leaf
   */
  def isLeaf: Boolean

  /**
   * List of all keys this object contain
   * @return list of keys
   */
  def keys: Set[String]

  /**
   * Return list of children as ``SchemaType``
   * @return list of children
   */
  def children: List[SchemaType]

  /**
   * Recursively extract all keys and subkeys from all objects in JSON schema
   * @return set of all keys that object contains
   */
  def extractAllKeys: Set[String] = {
    if (this.isLeaf)  Set.empty[String]
    else children.foldLeft(Set.empty[String])((acc, schemaType) => {
      schemaType.keys ++ schemaType.extractAllKeys ++ acc
    }) ++ keys
  }
}

/**
 * Companion object for all schema types
 */
object SchemaType extends SchemaHelper {
  /**
   * First try to extract schema for object type, if not found try to extract
   * primitive type
   *
   * @param json Object describing JSON Schema type
   * @return
   */
  def getFrom(json: JValue): Option[SchemaType] = json match {
    case JObject(o) if containsType(json, "object") => json.extractOpt[ObjectType]
    case JObject(_) => json.extractOpt[PrimitiveType]
    case _ => None
  }
}

/**
 * Container for all types except object
 *
 * @param `type`
 */
case class PrimitiveType(`type`: JValue) extends SchemaType {
  def isLeaf = true
  def keys = Set.empty[String]            // primitive type cannot contain any keys
  def children = Nil                      // ...nor children
}

/**
 * Container type for all object types
 *
 * @param properties map key to JObject, where JObject can probably contain
 *                   JSON Schema ``PrimitiveType`` or another ``ObjectType``
 * @param additionalProperties currently always false
 * @param `type` always object
 */
case class ObjectType(properties: Map[String, JObject], additionalProperties: Boolean, `type`: JValue) extends SchemaType {
  def isLeaf = false
  def keys = properties.keys.toSet
  def children = properties.values.flatMap(SchemaType.getFrom(_)).toList
}

