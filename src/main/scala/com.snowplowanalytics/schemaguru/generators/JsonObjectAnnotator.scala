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
package com.snowplowanalytics.schemaguru.generators

import scalaz._
import Scalaz._

// json4s
import org.json4s._
import org.json4s.jackson.JsonMethods._

// This library
import com.snowplowanalytics.schemaguru.json.SchemaHelper

/**
 * Base tree-like type for all JSON Schema types
 */
sealed trait SchemaType {
  import SchemaType._

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
  def keys: List[String]

  /**
   * Return list of children as ``SchemaType``
   * @return list of children
   */
  def children: List[SchemaType]

  /**
   * Try to extract ``SchemaType`` by it's full path
   * @param path dot-separated path to object
   * @return
   */
  def extractByPath(path: String): Option[SchemaType]

  /**
   * Recursively get PathMap for whole type
   * From root to deepest children
   */
  def convertToPathMap: PathMap = {
    if (this.isLeaf) { Map.empty[String, List[String]] }
    else children.foldLeft(Map.empty[String, List[String]])((acc, schemaType) =>
      schemaType.convertToPathMap |+| currentChildren |+| secondChildren
    )
  }
  
  /**
   * Predicate function deciding whether specified by path object
   * contains some children objects
   * @param path
   * @return
   */
  def containObject(path: String): Boolean = getPathMap(path).size > 0

  /**
   * Get ``PathMap`` where all keys contains objects
   * @return PathMap without non-object types
   */
  def keysWithObjects: PathMap = currentChildren.map(kv => kv._1 -> kv._2.filter(containObject))

  /**
   * Get PathMap with only one (current) level
   */
  def currentChildren: PathMap = Map("." -> keys)

  def secondChildren: PathMap =
    keysWithObjects.flatMap(kv => {
      val subPahts = kv._2.map(getPathMap)
      if (subPahts.isEmpty) Nil
      else subPahts
    }).foldLeft(Map.empty[String, List[String]])(_ ++ _)

  /**
   * Get PathMap from specific path
   *
   * @param path dot-separated path
   */
  def getPathMap(path: String): PathMap = {
    this.extractByPath(path) match {
      case Some(x) if !x.isLeaf => Map(path -> x.keys)
      case _                    => Map.empty[String, List[String]]
    }
  }

  /**
   * Recursively extract all keys and subkeys from all objects in JSON schema
   * @return
   */
  def extractAllKeys: Set[String] = {
    if (this.isLeaf)  Set.empty[String]
    else children.foldLeft(Set.empty[String])((acc, schemaType) => {
      schemaType.keys.toSet ++ schemaType.extractAllKeys ++ acc
    }) ++ keys.toSet
  }
}

/**
 * Companion object for all schema types
 */
object SchemaType extends SchemaHelper {
  /**
   * Mapping JSON object key to it's children
   * Example: Map(. -> List(person, ip), person -> List(name, surname))
   */
  type PathMap = Map[String, List[String]]

  /**
   * Extract ``SchemaType`` from JValue
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
  def keys = Nil                          // primitive type cannot contain any keys
  def children = Nil                      // ...nor children
  def extractByPath(path: String) = None  // ...at all
}

/**
 * Container type for all object types
 * @param properties map key to JObject, where JObject can probably contain
 *                   JSON Schema ``PrimitiveType`` or another ``ObjectType``
 * @param additionalProperties currently always false
 * @param `type` always object
 */
case class ObjectType(properties: Map[String, JObject], additionalProperties: Boolean, `type`: JValue) extends SchemaType {
  def isLeaf = false
  def keys = properties.keys.toList
  def children = properties.values.flatMap(SchemaType.getFrom(_)).toList
  def extractByPath(jsonPath: String): Option[SchemaType] = {
    jsonPath.split("\\.").toList match {
      case Nil => Some(this)
      case property :: Nil  => this.properties.get(property) match {
        case Some(obj) => SchemaType.getFrom(obj)
        case None => None
      }
      case property :: tail  => this.properties.get(property) match {
        case Some(obj) => SchemaType.getFrom(obj).flatMap(_.extractByPath(tail.mkString(".")))
        case None => None
      }
    }
  }
}

