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
case class SeqEnvelope(seqNr: Long, sender: User, msg: Message, time: DateTime)

case class Join(r: RoomId) extends OperationRequest
case class Leave(r: RoomId) extends OperationRequest
case class BeginTyping(r: RoomId) extends RoomOp
case class StoppedTyping(r: RoomId) extends RoomOp
case class Away(r: RoomId) extends RoomOp
case class BroadcastMessageRequest(r: RoomId, data: Message) extends RoomOp //coming from user (REQUEST)
case class RoomMessage(r: RoomId, envelope: SeqEnvelope) //going to room (RESPONSE)

object DefaultFormats {
  implicit val joinFmt = Json.format[Join]
  implicit val leaveFmt = Json.format[Leave]
  implicit val btFmt = Json.format[BeginTyping]
  implicit val stFmt = Json.format[StoppedTyping]
  implicit val awayFmt = Json.format[Away]
  implicit val msgFmt = Json.format[Message]
//  implicit val seqEnvFmt = Json.format[SeqEnvelope]
  implicit val seqEnvWrites = new Writes[SeqEnvelope] {
   def writes(env: SeqEnvelope): JsValue =
     Json.obj(
       "#" -> env.seqNr,
       "sender" -> env.sender,
       "time" -> env.time
     ) ++ Json.toJson(env.msg).as[JsObject]
}
  implicit val roomWrite = Json.writes[RoomMessage]
  implicit val broadcastMsgFmt = Json.format[BroadcastMessageRequest]

}