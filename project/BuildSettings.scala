/*
 * Copyright (c) 2016 Snowplow Analytics Ltd. All rights reserved.
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
  // Common settings for all our projects
  lazy val commonSettings = Seq[Setting[_]](
    organization          :=  "com.snowplowanalytics",
    version               :=  "0.6.1-rc1",
    scalaVersion          :=  "2.10.6",
    crossScalaVersions    :=  Seq("2.10.6", "2.11.7"),
    scalacOptions         :=  Seq("-deprecation", "-encoding", "utf8",
                                  "-unchecked", "-feature",
                                  "-Xfatal-warnings", "-target:jvm-1.7"),
    scalacOptions in Test :=  Seq("-Yrangepos"),
    resolvers             ++= Dependencies.resolutionRepos
  )

  // Settings specific for Schema Guru CLI
  lazy val coreSettings = Seq[Setting[_]](
    description           :=  "For deriving JSON Schemas from collections of JSON instances",

    mainClass in (Compile, run) := Some("com.snowplowanalytics.schemaguru.Main")
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
  lazy val sbtAssemblyCommonSettings = assemblySettings ++ Seq(
    // Executable jarfile
    assemblyOption in assembly ~= { _.copy(prependShellScript = Some(defaultShellScript)) },

    // Name it as an executable
    jarName in assembly := { s"${name.value}-${version.value}" }
  )

  lazy val sbtAssemblyCoreSettings = sbtAssemblyCommonSettings ++ Seq(
    // Drop these jars
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      val excludes = Set(
        "commons-beanutils-1.8.3.jar" // Clashes with commons-collections
      )
      cp filter { jar => excludes(jar.data.getName) }
    },
    mainClass in assembly := Some("com.snowplowanalytics.schemaguru.Main")
  )

  lazy val coreBuildSettings =
    commonSettings ++
    coreSettings ++
    scalifySettings ++
    sbtAssemblyCoreSettings
}
