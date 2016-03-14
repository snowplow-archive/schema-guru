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
package webui

// Akka & Spray
import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import spray.can.Http

// Argot
import scopt._

object WebuiCli {
  case class Config(
    port: Int = 8000,
    interface: String = "0.0.0.0"
  )

  val parser = new OptionParser[Config]("schema-guru") {
    head(generated.ProjectSettings.name, generated.ProjectSettings.version)
    note("some notes.\n")
    help("help") text("prints this usage text")
    opt[Int]("port") action { (x, c) =>
      c.copy(port = x) } text("TCP port to run Web UI (default: 8000)")
    opt[String]("interface") action { (x, c) =>
      c.copy(interface = x) } text("Interface to bind Web UI (default: 0.0.0.0)")
  }
}

class SchemaGuruRoutesActor extends SchemaGuruRoutes with Actor {
  def actorRefFactory = context
  def receive = runRoute(rootRoute)
}

object Main extends App {
  val config = WebuiCli.parser.parse(args, WebuiCli.Config())

  config match {
    case Some(options) => run(options)
    case None => WebuiCli.parser.showUsageAsError()
  }

  def run(options: WebuiCli.Config): Unit = {
    // we need an ActorSystem to host our service
    implicit val system = ActorSystem()

    //create our service actor
    val service = system.actorOf(Props[SchemaGuruRoutesActor], "schema-guru-service")

    //bind our actor to an HTTP port
    IO(Http) ! Http.Bind(service, interface = options.interface, port = options.port)
  }
}
