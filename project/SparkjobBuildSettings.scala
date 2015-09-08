/*
 * Copyright (c) 2015 Snowplow Analytics Ltd. All rights reserved.
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
import spray.revolver.RevolverPlugin.Revolver

object SparkjobBuildSettings {
  import BuildSettings._

  // Settings specific for Schema Guru web UI
  lazy val sparkjobSettings = Seq[Setting[_]](
    description           :=  "Spark job for Schema derivation",

    mainClass in (Compile, run) := Some("com.snowplowanalytics.schemaguru.sparkjob.Main")
  )

  // scalifySettings with changed package, since it clashes in assembly
  lazy val scalifySparkjobSettings = Seq(sourceGenerators in Compile <+= (sourceManaged in Compile, version, name, organization, scalaVersion) map { (d, v, n, o, sv) =>
    val file = d / "settings.scala"
    IO.write(file, """package com.snowplowanalytics.schemaguru.sparkjob.generated
                     |object ProjectSettings {
                     |  val version = "%s"
                     |  val name = "%s"
                     |  val organization = "%s"
                     |  val scalaVersion = "%s"
                     |}
                     |""".stripMargin.format(v, n, o, sv))
    Seq(file)
  })

  import sbtassembly.Plugin._
  import AssemblyKeys._
  lazy val sbtAssemblySparkjobSettings = sbtAssemblyCommonSettings ++ Seq(
    // Drop these jars
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      val excludes = Set(
        "commons-beanutils-1.8.3.jar"
      )
      cp filter { jar => excludes(jar.data.getName) }
    },
    mainClass in assembly := Some("com.snowplowanalytics.schemaguru.sparkjob.Main")
  )

  lazy val sparkjobBuildSettings =
    commonSettings ++
    sparkjobSettings ++
    scalifySparkjobSettings ++
    sbtAssemblySparkjobSettings ++
    Revolver.settings
}
