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
package im.instalk.actors

import akka.actor._
import im.instalk.User
import im.instalk.protocol._
import im.instalk.actors.RoomManager.JoinOrCreate
import play.api.libs.json._
import org.joda.time.DateTime
import im.instalk.store.Persistence

object Room {

  case class RoomJoined(r: RoomId, members: Iterable[User], lastMessages: Iterable[JsObject])

  case class RoomLeft(r: RoomId)

}


class Room(roomId: RoomId, persistence: Persistence) extends Actor with ActorLogging {
  log.info("Room {} created", roomId)
  var seqNr = persistence.getNextAvailableSeqNr(roomId).getOrElse(0l)
  var topic = persistence.getTopic(roomId).getOrElse("")
  var members = Map.empty[ActorRef, User]
  def lastMessages: Iterable[JsObject] = persistence.getLatestMessages(roomId, seqNr)

  def receive = {
    case JoinOrCreate(_, u) =>
      //send him welcome message
      sender ! Room.RoomJoined(roomId, members.values, lastMessages)
      publish(Responses.joinedRoom(roomId, u, DateTime.now()))
      //put the guy in the members
      members += (sender -> u)
      context watch sender
    case Leave(_) =>
      if (members.contains(sender)) {
        val user = members(sender)
        members -= sender
        context unwatch sender
        sender ! Room.RoomLeft(roomId)
        publish(Responses.leftRoom(roomId, user, DateTime.now()))
      }

    case o: BroadcastMessageRequest =>
      members.get(sender) match {
        case Some(user) =>
          val data = Responses.roomMessage(RoomMessage(roomId, SeqEnvelope(seqNr, user, o.data, DateTime.now())))
          publish(data)
          persistence.storeEvent(roomId, seqNr, "msg", data, topic,  members.values)
          seqNr += 1
        case None =>
          send(sender, Errors.notMemberInRoom)
      }
    case Terminated(who) =>
      //somebody left, advertise leaving
      members.get(who).foreach {
        u =>
          members -= who
          publish(Responses.leftRoom(roomId, u, DateTime.now()))
      }
      if (members.isEmpty)
        context stop self
  }

  def publish(msg: JsObject): Unit = {
    members.foreach {
      case (k, v) => send(k, msg)
    }
    log.debug("Room '{}' Message '{}'", roomId, msg)
  }

  def send(to: ActorRef, msg: JsObject): Unit =
    to ! Client.Response(msg)

}