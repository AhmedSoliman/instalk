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
package im.instalk.test.util

import play.api.libs.json.{JsObject, Json}
import im.instalk.test.util.WebSocketClient.Messages.TextMessage
import im.instalk.User
import akka.testkit.{TestProbe, TestKit}
import org.scalatest._
import scala.concurrent.duration._
import im.instalk.protocol.{Message, RoomId}
import org.joda.time.DateTime

trait Helpers {
  this: TestKit with Suite with Matchers with FlatSpecLike =>

  import im.instalk.protocol.DefaultFormats._

  def initialise(socket: WebSocketClient, probe: TestProbe): User = {
    socket.send(Json.obj("v" -> "0.1"))
    probe.expectMsgPF(3.seconds) {
      case TextMessage(_, m) =>
        val msg = Json.parse(m)
        (msg \ "welcome").as[Int] should equal(1)
        val user = (msg \ "user").as[User]
//        user.color should startWith("#")
//        user.color should have size (7)
        user.username should startWith("Anonymous-")
        user
    }
  }

  def joinRoom(r: RoomId, socket: WebSocketClient, probe: TestProbe): (List[User], List[JsObject]) = {
    socket.send(Json.obj(
      "r" -> r,
      "o" -> "join"
    ))
    probe.expectMsgPF(5.seconds) {
      case TextMessage(socket, msg) =>
        val response = Json.parse(msg)
        (response \ "r").as[String] should equal(r)
        (response \ "o").as[String] should equal("room-welcome")
        ((response \ "data" \ "members").as[List[User]], (response \ "data" \ "messages").as[List[JsObject]])
    }
  }

  def leaveRoom(r: RoomId, socket: WebSocketClient, probe: TestProbe): Unit = {
    socket.send(Json.obj(
      "r" -> r,
      "o" -> "leave"
    ))
    probe.expectMsgPF(5.seconds) {
      case TextMessage(socket, msg) =>
        val response = Json.parse(msg)
        (response \ "r").as[String] should equal(r)
        (response \ "o").as[String] should equal("room-bye")
    }
  }

  def toJson(msg: String): JsObject = Json.parse(msg).as[JsObject]

  def matchJoined(msg: JsObject, r: RoomId, user: User): Unit = {
    //{"r":"Soliman","o":"joined","data":{"user":{"color":"#2A35E3","username":"Anonymous-4646"},"when":1398513302522}}
    (msg \ "r").as[RoomId] should equal(r)
    (msg \ "o").as[String] should equal("joined")
    (msg \ "data" \ "user").as[User] should equal(user)
    (msg \ "data" \ "when").as[Long] should (be < DateTime.now.getMillis and be > DateTime.now.getMillis - 5000)
  }

  def matchLeft(msg: JsObject, r: RoomId, user: User): Unit = {
    //{"r":"Soliman","o":"joined","data":{"user":{"color":"#2A35E3","username":"Anonymous-4646"},"when":1398513302522}}
    (msg \ "r").as[RoomId] should equal(r)
    (msg \ "o").as[String] should equal("left")
    (msg \ "data" \ "user").as[String] should equal(user.username)
    (msg \ "data" \ "when").as[Long] should (be < DateTime.now.getMillis and be > DateTime.now.getMillis - 5000)
  }

  def sendMessage(room: RoomId, socket: WebSocketClient, message: Message): Unit = {
    socket.send(Json.toJson(
      Json.obj(
        "r" -> room,
        "o" -> "msg",
        "data" -> message
      )
    ))
  }

  def receiveMessage(room: RoomId, probe: TestProbe): (User, Long, Message) = {
    probe.expectMsgPF(5.seconds) {
      case TextMessage(_, m) =>
        val response = Json.parse(m).as[JsObject]
        (response \ "r").asOpt[RoomId] should be(Some(room))
        (response \ "o").asOpt[String] should be(Some("msg"))
        ((response \ "data" \ "msg" \ "sender").as[User],
          (response \ "data" \ "msg" \ "#").as[Long],
          (response \ "data" \ "msg").as[Message])

    }
  }

  def receiveEvent(room: RoomId, op: String, probe: TestProbe): User = {
    probe.expectMsgPF(5.seconds) {
      case TextMessage(_, m) =>
        val response = Json.parse(m).as[JsObject]
        (response \ "r").asOpt[RoomId] should be(Some(room))
        (response \ "o").asOpt[String] should be(Some(op))
        (response \ "data" \ "user").as[User]
    }

  }

  def fetchBefore(r: RoomId, before: Long, socket: WebSocketClient, probe: TestProbe): List[JsObject] = {
    socket.send(Json.obj(
      "r" -> r,
      "o" -> "fetch",
      "data" -> Json.obj("before" -> before)
    ))
    probe.expectMsgPF(5.seconds) {
      case TextMessage(socket, msg) =>
        val response = Json.parse(msg)
        (response \ "r").as[String] should equal(r)
        (response \ "o").as[String] should equal("fetch")
        (response \ "data" \ "messages").as[List[JsObject]]
    }
  }

  def chatEvent(r: RoomId, e: String, socket: WebSocketClient, probe: TestProbe): String = {
    socket.send(Json.obj(
      "r" -> r,
      "o" -> e
    ))
    expectChatEventMessage(r, e, socket, probe)
  }

  def expectChatEventMessage(r: RoomId, e: String, socket: WebSocketClient, probe: TestProbe): String = {
    probe.expectMsgPF(5.seconds) {
    case TextMessage(socket, msg) =>
    val response = Json.parse(msg)
    (response \ "r").as[String] should equal(r)
    (response \ "o").as[String] should equal(e)
    (response \ "data" \ "when").as[Long] should (be < DateTime.now.getMillis and be > DateTime.now.getMillis - 5000)
    (response \ "data" \ "sender").as[String]
  }

  }

}