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

// Akka & Spray
import akka.actor.{Actor, ActorSystem, Props}
import akka.io.IO
import spray.can.Http

class SchemaGuruRoutesActor extends SchemaGuruRoutes with Actor {
  def actorRefFactory = context
  def receive = runRoute(rootRoute)
}

object WebApp extends App {
  // we need an ActorSystem to host our service
  implicit val system = ActorSystem()

  //create our service actor
  val service = system.actorOf(Props[SchemaGuruRoutesActor], "schema-guru-service")

  //bind our actor to an HTTP port
  IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = 8000)
}
