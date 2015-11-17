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

// This library
import schema.Helpers.SchemaContext

class StringPatternAnnotationSpec extends Specification { def is = s2"""
  Check string type annotations
    recognize base64 on big quantity               $recognizeBase64
    don't recognize base64 on small quantity       $doNotRecognizeBase64OnSmallQuantity
    don't recognize invalid base64                 $doNotRecognizeIncorrectBase64
    recognize long base64 even on small quantity   $recognizeLongBase64
    annotate field with base64 pattern             $annotateFieldWithBase64
    don't annotate if base64 is invalid            $doNotAnnotateFieldWithBase64
    """

  val generatorForSmallQuantity = SchemaGenerator(SchemaContext(0, quantity = Some(1)))
  val generatorForBigQuantity = SchemaGenerator(SchemaContext(0, quantity = Some(10000)))

  val base64Regexp = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$"

  val base64 = "aGVsbG8="
  val longBase64 = "VGhpcyBzdHJpbmcgc2hvdWxkIGJlIG1vcmUgdGhhbiAzMiBzeW1ib2xzIGluIGJhc2U2NA=="
  val incorrectBase64 = "aVsbG="

  def recognizeBase64 =
    generatorForBigQuantity.Annotations.suggestBase64Pattern(base64) must beSome(base64Regexp)

  def doNotRecognizeBase64OnSmallQuantity =
    generatorForSmallQuantity.Annotations.suggestBase64Pattern(base64) must beNone

  def doNotRecognizeIncorrectBase64 =
    generatorForBigQuantity.Annotations.suggestBase64Pattern(incorrectBase64) must beNone

  def recognizeLongBase64 =
    generatorForSmallQuantity.Annotations.suggestBase64Pattern(longBase64) must beSome(base64Regexp)

  def annotateFieldWithBase64 =
    generatorForBigQuantity.Annotations.annotateString(base64).pattern must beSome(base64Regexp)

  def doNotAnnotateFieldWithBase64 =
    generatorForSmallQuantity.Annotations.annotateString(base64).pattern must beNone

}

