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
package cli

// specs2
import org.specs2.Specification

import com.snowplowanalytics.schemaddl.SchemaData.SelfDescInfo

class CliSpec extends Specification { def is = s2"""
  Check CLI specification
    generate DDL filename  $e1
  """

  def e1 = {
    val fileName = ("com.acme", "event_name_1")
    DdlCommand.getFileName(SelfDescInfo("com.acme", "event_name", "1-00-0")) must beEqualTo(fileName) and(
      DdlCommand.getFileName(SelfDescInfo("com.acme", "event_name", "1-10-0")) must beEqualTo(fileName) and(
        DdlCommand.getFileName(SelfDescInfo("com.acme", "event_name", "1-0-10")) must beEqualTo(fileName) and(
          DdlCommand.getFileName(SelfDescInfo("com.acme", "event_name", "1-22-3")) must beEqualTo(fileName))))
  }
}
