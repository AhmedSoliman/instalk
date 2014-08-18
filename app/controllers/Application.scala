/*
 * Copyright 2014 The Instalk Project
 *
 * The Instalk Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package controllers

import play.api.mvc._
import play.api.libs.json._
import im.instalk.global.Instalk
import scala.concurrent.duration._
import scala.util.Right
import akka.pattern.ask
import akka.util.Timeout
import im.instalk.actors.{ ClientManager, WebSocketActor}

object Application extends Controller {

  import play.api.Play.current
  val globals = current.global.asInstanceOf[Instalk.type]
  implicit val ec = globals.actorSystem.dispatcher
  implicit val to = Timeout(10.seconds)
  def index = Action {
    Ok("Go Away!")
  }

  def websocket = WebSocket.tryAccept[String] {
    request =>
        val act = (globals.clientManager ? ClientManager.CreateClient(request)).mapTo[WebSocketActor.Actuator]
        act.map(x => Right(x.act))
  }
}