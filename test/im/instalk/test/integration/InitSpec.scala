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
package im.instalk.test.integration

import im.instalk.test.util.PlayServer
import im.instalk.test.util.WebSocketClient.Messages._
import im.instalk.User
import org.scalatest._
import akka.testkit._
import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class InitSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FlatSpecLike
with Matchers with PlayServer {

  def this() = this(ActorSystem("ProtocolSpecSystem"))

  "Initialisation" should "react with welcome message after sending client version" in {
    withSocket(false) {
      (socket, probe) =>
        socket.send(Json.obj("v" -> "0.1"))
        probe.expectMsgPF(3.seconds) {
          case TextMessage(_, m) =>
            val msg = Json.parse(m)
            (msg \ "welcome").as[Int] should equal(1)
            val user = (msg \ "user").as[User]
            user.color should startWith("#")
            user.color should have size(7)
            user.username should startWith("Anonymous-")
        }
    }
  }
}