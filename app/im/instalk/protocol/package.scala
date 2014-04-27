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
package im.instalk

import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime

package object protocol {

  import DefaultFormats._

  val VERSION = "0.1"
  type Version = String
  type SessionId = String
  type RoomId = String

  implicit val operation: Reads[OperationRequest] = new Reads[OperationRequest] {
    def reads(o: JsValue): JsResult[OperationRequest] = {
      (o \ "o").asOpt[String] match {
        case Some("join") =>
          Json.fromJson[Join](o)
        case Some("leave") =>
          Json.fromJson[Leave](o)
        case Some("bt") =>
          Json.fromJson[BeginTyping](o)
        case Some("st") =>
          Json.fromJson[StoppedTyping](o)
        case Some("away") =>
          Json.fromJson[Away](o)
        case Some("msg") =>
          Json.fromJson[BroadcastMessageRequest](o)
        case Some(_) =>
          JsError("operation.unknown")
        case None =>
          JsError("operation.missing")
      }
    }
  }

  object Validators {
    val version: Reads[Version] = (__ \ 'v).read[String]
    val heartbeat: Reads[Int] = (__ \ "heart-beat").read[Int]
  }

  object Responses {
    def welcome(user: User) = Json.obj("welcome" -> 1, "user" -> user)

    val timeout = Json.obj("timeout" -> 1)

    def heartbeatAck(i: Int) = Json.obj("heart-beat-ack" -> i)

    def notSupportedOp(op: String) = Json.obj("o" -> op) ++ Errors.unsupportedOp

    def roomWelcome(roomId: RoomId, members: Iterable[User]) = Json.obj(
      "r" -> roomId,
      "o" -> "room-welcome",
      "data" -> Json.obj(
        "members" -> Json.toJson(members)
      )
    )

    def roomBye(roomId: RoomId) = Json.obj(
      "r" -> roomId,
      "o" -> "room-bye"
    )

    def joinedRoom(roomId: RoomId, who: User, when: DateTime) = Json.obj(
      "r" -> roomId,
      "o" -> "joined",
      "data" -> Json.obj(
        "user" -> who,
        "when" -> when
      )
    )

    def leftRoom(roomId: RoomId, who: User, when: DateTime) = Json.obj(
      "r" -> roomId,
      "o" -> "left",
      "data" -> Json.obj(
        "user" -> who,
        "when" -> when
      )
    )

    def roomMessage(msg: RoomMessage): JsObject =
      Json.obj(
        "r" -> msg.r,
        "o" -> "msg",
        "data" -> Json.obj(
          "msg" -> msg.envelope
        )
      )
  }

  object Errors {
    private[this] def formatError(code: Int, msg: String) = Json.obj("error" -> Json.obj("code" -> code, "msg" -> msg))

    private[this] def formatJsonError(code: Int, msg: String, e: JsError) =
      Json.obj("error" -> Json.obj("code" -> code, "msg" -> msg, "errors" -> JsError.toFlatJson(e)))

    val InvalidVersion = formatError(1, "version.invalid")
    val unsupportedOp = formatError(2, "operation.unsupported")
    val missingOperation = formatError(3, "operation.missing")
    val notImplemented = formatError(999, "implementation.missing")
    val unknownRoom = formatError(4, "room.unknown")
    val notMemberInRoom = formatError(5, "room.member.notfound")
    val badJson = formatError(401, "json.malformed")

    def invalidOperationMessage(e: JsError) = formatJsonError(400, "request.bad", e)
  }

}