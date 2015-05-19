/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the
 * Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.  See the Apache License Version 2.0 for the specific
 * language governing permissions and limitations there under.
 */
import sbt._
import Keys._

object BuildSettings {

  // Basic settings for our app
  lazy val basicSettings = Seq[Setting[_]](
    organization          :=  "com.snowplowanalytics",
    version               :=  "0.1.0-M1",
    description           :=  "For deriving JSON Schemas from collections of JSON instances",
    scalaVersion          :=  "2.10.5",
    scalacOptions         :=  Seq("-deprecation", "-encoding", "utf8",
                                  "-unchecked", "-feature",
                                  "-target:jvm-1.7"),
    scalacOptions in Test :=  Seq("-Yrangepos"),
    resolvers             ++= Dependencies.resolutionRepos
  )

  // Makes our SBT app settings available from within the ETL
  lazy val scalifySettings = Seq(sourceGenerators in Compile <+= (sourceManaged in Compile, version, name, organization, scalaVersion) map { (d, v, n, o, sv) =>
    val file = d / "settings.scala"
    IO.write(file, """package com.snowplowanalytics.schemaguru.generated
      |object ProjectSettings {
      |  val version = "%s"
      |  val name = "%s"
      |  val organization = "%s"
      |  val scalaVersion = "%s"
      |}
      |""".stripMargin.format(v, n, o, sv))
    Seq(file)
  })

  // sbt-assembly settings for building a fat jar
  import sbtassembly.Plugin._
  import AssemblyKeys._
  lazy val sbtAssemblySettings = assemblySettings ++ Seq(

    // Simpler jar name
    jarName in assembly := {
      name.value + ".jar"
    },

    // Make this executable
    mainClass in assembly := Some("com.snowplowanalytics.schemaguru.SchemaGuruApp")
  )

  lazy val buildSettings = basicSettings ++ scalifySettings ++ sbtAssemblySettings
}
