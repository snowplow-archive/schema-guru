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

class StringFormatAnnotationSpec extends Specification { def is = s2"""
  Check string format annotations
    recognize UUID                            $recognizeUuid
    recognize ISO date                        $recognizeIsoDate
    skip invalid date                         $doNotRecognizeIncorrectDate
    skip invalid date as number               $doNotRecognizeIncorrectDateAsNum
    add date-time format to string            $annotateFieldWithDate
    add ipv4 format to string                 $annotateFieldWithIp4
    add uri format to string                  $annotateFieldWithUri
    """

  val schemaContext = schema.Helpers.SchemaContext(0)
  val generator = SchemaGenerator(schemaContext)

  val correctUuid = "f0e89550-7fda-11e4-bbe8-22000ad9bf74"
  val correctDate = "2010-01-01T12:00:00+01:00"
  val correctUri = "https://github.com/snowplow/schema-guru"
  val incorrectDate = "2010-13-01T12:00:00+01:00"
  val incorrectDateAsNumString = "23"
  val correctIp = "192.1.1.2"

  def recognizeUuid =
    generator.Annotations.suggestUuidFormat(correctUuid) must beSome("uuid")

  def recognizeIsoDate =
    generator.Annotations.suggestTimeFormat(correctDate) must beSome("date-time")

  def doNotRecognizeIncorrectDate =
    generator.Annotations.suggestTimeFormat(incorrectDate) must beNone

  def doNotRecognizeIncorrectDateAsNum =
    generator.Annotations.suggestTimeFormat(incorrectDateAsNumString) must beNone

  def annotateFieldWithDate =
    generator.Annotations.annotateString(correctDate).format must beSome("date-time")

  def annotateFieldWithIp4 =
    generator.Annotations.annotateString(correctIp).format must beSome("ipv4")

  def annotateFieldWithUri =
    generator.Annotations.annotateString(correctUri).format must beSome("uri")
}

