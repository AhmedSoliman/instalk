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
package im.instalk.protocol

import play.api.libs.json._
import im.instalk.User
import org.joda.time.DateTime

sealed trait OperationRequest {
  def r: RoomId
}

sealed trait RoomOp extends OperationRequest

case class Message(txt: String)

case class RoomTopicMessage(`#`: Long, topic: String, sender: User, when: DateTime)

case class SeqEnvelope(seqNr: Long, sender: User, msg: Message, time: DateTime)

case class FetchBefore(before: Long)

case class UserInfoModification(`#`: Long, originalUsername: String, newUserInfo: User, when: DateTime)

case class Join(r: RoomId) extends OperationRequest

case class Leave(r: RoomId) extends OperationRequest

case class AnonymousInfoModification(name: Option[String], color: Option[String])

case class SetUserInfoRequest(r: RoomId, data: AnonymousInfoModification) extends OperationRequest

case class BeginTyping(r: RoomId) extends RoomOp

case class StoppedTyping(r: RoomId) extends RoomOp

case class Away(r: RoomId) extends RoomOp

case class BroadcastMessageRequest(r: RoomId, data: Message) extends RoomOp

case class SetRoomTopicResponse(r: RoomId, data: RoomTopicMessage) extends RoomOp

//coming from user (REQUEST)
case class RoomMessage(r: RoomId, envelope: SeqEnvelope)

case class NewTopic(topic: String)

case class SetRoomTopicRequest(r: RoomId, data: NewTopic) extends RoomOp

//going to room (RESPONSE)
case class Fetch(r: RoomId, data: FetchBefore) extends RoomOp

case class SetUserInfo(r: RoomId, data: UserInfoModification)

object DefaultFormats {
  implicit val joinFmt = Json.format[Join]
  implicit val leaveFmt = Json.format[Leave]
  implicit val btFmt = Json.format[BeginTyping]
  implicit val stFmt = Json.format[StoppedTyping]
  implicit val awayFmt = Json.format[Away]
  implicit val anonInfoModFmt = Json.format[AnonymousInfoModification]
  implicit val setUserInfoReqFmt = Json.format[SetUserInfoRequest]
  implicit val userInfoModFmt = Json.format[UserInfoModification]
  implicit val msgFmt = Json.format[Message]
  implicit val rtmFmt = Json.format[RoomTopicMessage]
  implicit val newTopicFmt = Json.format[NewTopic]
  implicit val srtReqFmt = Json.format[SetRoomTopicRequest]


  implicit val setRoomTopicWrites = new Writes[SetRoomTopicResponse] {
    def writes(resp: SetRoomTopicResponse): JsValue =
      Json.obj(
        "r" -> resp.r,
        "o" -> "set-room-topic",
        "data" -> Json.toJson(resp.data)
      )
  }
  implicit val seqEnvWrites = new Writes[SeqEnvelope] {
    def writes(env: SeqEnvelope): JsValue =
      Json.obj(
        "#" -> env.seqNr,
        "sender" -> env.sender,
        "time" -> env.time
      ) ++ Json.toJson(env.msg).as[JsObject]
  }

  implicit val setUserInfoWrites = new Writes[SetUserInfo] {
    def writes(setInfo: SetUserInfo): JsValue =
      Json.obj(
        "r" -> setInfo.r,
        "o" -> "set-user-info",
        "data" -> Json.toJson(setInfo.data)
      )
  }
  implicit val roomWrite = Json.writes[RoomMessage]
  implicit val broadcastMsgFmt = Json.format[BroadcastMessageRequest]
  implicit val fetchBeforeFmt = Json.format[FetchBefore]
  implicit val fetchFmt = Json.format[Fetch]
}
