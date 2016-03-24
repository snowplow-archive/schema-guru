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
package sparkjob

import Common.DerivedSchema

/**
 * Case class containing all information for output.
 * Result, path to output it, errors happened during processing set of
 * related instances and path to output these errors
 *
 * @param schema end result of all transformations
 * @param schemaPath path to output result
 * @param errors errors happened during processing related instances
 * @param errorsPath path to output errors
 */
// TODO: use it in CLI
case class OutputResult(schema: DerivedSchema, schemaPath: Option[String], errors: List[String], errorsPath: Option[String])

