/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
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
import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    // For Snowplow libs
    "Snowplow Analytics Maven repo" at "http://maven.snplow.com/releases/",
    "Snowplow Analytics Maven snapshot repo" at "http://maven.snplow.com/snapshots/"
  )

  object V {
    // Java
    val yodaTime         = "2.1"
    val yodaConvert      = "1.2"
    val jacksonDatabind  = "2.5.4"
    val jsonValidator    = "2.2.3"
    val commonsValidator = "1.4.1"
    // Scala
    val argot            = "1.0.3"
    val scalaz7          = "7.0.6"
    val json4s           = "3.2.10"   // don't upgrade to 3.2.11 https://github.com/json4s/json4s/issues/212
    val jsonpath         = "0.6.4"
    val akka             = "2.3.9"
    val spray            = "1.3.3"
    val spark            = "1.3.1"
    // Scala (test only)
    val specs2           = "2.3.13"
    val scalazSpecs2     = "0.2"
    val scalaCheck       = "1.12.2"
    val schemaddl     	 = "0.2.0-M1"
  }

  object Libraries {
    // Java
    val yodaTime         = "joda-time"                  %  "joda-time"                 % V.yodaTime
    val yodaConvert      = "org.joda"                   %  "joda-convert"              % V.yodaConvert
    val jacksonDatabind  = "com.fasterxml.jackson.core" %  "jackson-databind"          % V.jacksonDatabind
    val jsonValidator    = "com.github.fge"             %  "json-schema-validator"     % V.jsonValidator
    val commonsValidator = "commons-validator"          %  "commons-validator"         % V.commonsValidator
    // Scala
    val argot            = "org.clapper"                %% "argot"                     % V.argot
    val scalaz7          = "org.scalaz"                 %% "scalaz-core"               % V.scalaz7
    val json4sJackson    = "org.json4s"                 %% "json4s-jackson"            % V.json4s
    val json4sScalaz     = "org.json4s"                 %% "json4s-scalaz"             % V.json4s
    val jsonpath         = "io.gatling"                 %% "jsonpath"                  % V.jsonpath
    val schemaddl        = "com.snowplowanalytics"      %% "schema-ddl"                % V.schemaddl
    // Spray
    val akka             = "com.typesafe.akka"          %% "akka-actor"                % V.akka
    val sprayCan         = "io.spray"                   %% "spray-can"                 % V.spray
    val sprayRouting     = "io.spray"                   %% "spray-routing"             % V.spray
    // Spark
    val sparkCore        = "org.apache.spark"           %% "spark-core"                % V.spark          % "provided"
    // Scala (test only)
    val specs2           = "org.specs2"                 %% "specs2"                    % V.specs2         % "test"
    val scalazSpecs2     = "org.typelevel"              %% "scalaz-specs2"             % V.scalazSpecs2   % "test"
    val scalaCheck       = "org.scalacheck"             %% "scalacheck"                % V.scalaCheck     % "test"
    val sprayTestkit     = "io.spray"                   %% "spray-testkit"             % V.spray          % "test"
  }
}
