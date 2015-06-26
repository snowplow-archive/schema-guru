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

// specs2
import org.specs2.Specification

// This project
import JsonSchemaGenerator.Annotations

class StringPatternAnnotationSpec extends Specification { def is = s2"""
  Check string type annotations
    recognize base63                          $recognizeBase64
    do not recognize invalid base64           $doNotRecognizeIncorrectBase64
    annotate field with base64 pattern        $annotateFieldWithBase64
    """

  val base64Regexp = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$"

  val base64 = "aGVsbG8="
  val incorrectBase64 = "aVsbG="

  def recognizeBase64 =
    Annotations.suggestBase64Pattern(base64) must beSome(base64Regexp)

  def doNotRecognizeIncorrectBase64 =
    Annotations.suggestTimeFormat(incorrectBase64) must beNone

  def annotateFieldWithBase64 =
    Annotations.annotateString(base64).values must havePair(("pattern", base64Regexp))
}

