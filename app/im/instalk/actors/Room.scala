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
import im.instalk.actors.Room.UpdateUser

object Room {

  case class RoomJoined(r: RoomId, members: Iterable[User], lastMessages: Iterable[JsObject], topic: String)

  case class UpdateUser(newUser: User)

  case class RoomLeft(r: RoomId)

}


class Room(roomId: RoomId, persistence: Persistence) extends Actor with ActorLogging {
  log.info("Room {} created", roomId)
  var seqNr = persistence.getNextAvailableSeqNr(roomId).getOrElse(0l)

  def topic = persistence.getTopic(roomId).getOrElse("")

  var members = Map.empty[ActorRef, User]

  def lastMessages: Iterable[JsObject] = persistence.getLatestMessages(roomId)

  def receive = {
    case JoinOrCreate(_, u) =>
      //send him welcome message
      sender ! Room.RoomJoined(roomId, members.values.toSet - u, lastMessages, topic)
      if (!members.values.exists(_ == u))
        publish(Responses.joinedRoom(roomId, u, DateTime.now()))
      //put the guy in the members
      members += (sender -> u)
      context watch sender
    case Leave(_) =>
      leave(true, sender)
    case UpdateUser(newUser) =>
      members.get(sender).map {
        oldUser =>
          members -= sender
          members += (sender -> newUser)

          //update the other actors with the old user still
          members.foreach {
            case (client, u) =>
              if (u == oldUser) {
                members -= client
                members += (client -> newUser)
                client ! UpdateUser(newUser)
              }
          }

          //publish that we changed the guy
          val data = Responses.setUserInfo(SetUserInfo(roomId, UserInfoModification(seqNr, oldUser.username, newUser, DateTime.now())))
          persistence.storeEvent(roomId, seqNr, "set-user-info", data, topic, members.values)
          seqNr += 1
          publish(data)

      }
    case o: BroadcastMessageRequest =>
      members.get(sender) match {
        case Some(user) =>
          val data = Responses.roomMessage(RoomMessage(roomId, SeqEnvelope(seqNr, user, o.data, DateTime.now())))
          publish(data)
          persistence.storeEvent(roomId, seqNr, "msg", data, topic, members.values)
          seqNr += 1
        case None =>
          send(sender, Errors.notMemberInRoom)
      }
    case srt: SetRoomTopicRequest =>
      val data = Responses.setRoomTopic(SetRoomTopicResponse(roomId, RoomTopicMessage(seqNr, srt.data.topic, members(sender), DateTime.now())))
      persistence.storeEvent(roomId, seqNr, "set-room-topic", data, topic, members.values)
      persistence.setTopic(roomId, srt.data.topic)
      seqNr += 1
      publish(data)
    case o: BeginTyping =>
      members.get(sender) match {
        case Some(user) =>
          publish(Responses.beginTyping(roomId, user))
        case None =>
          send(sender, Errors.notMemberInRoom)
      }
    case o: StoppedTyping =>
      members.get(sender) match {
        case Some(user) =>
          publish(Responses.stoppedTyping(roomId, user))
        case None =>
          send(sender, Errors.notMemberInRoom)
      }
    case o: Away =>
      members.get(sender) match {
        case Some(user) =>
          publish(Responses.away(roomId, user))
        case None =>
          send(sender, Errors.notMemberInRoom)
      }
    case Terminated(who) =>
      //somebody left, advertise leaving
      leave(false, who)
    case Fetch(r, data) =>
      val result = Responses.fetchBefore(roomId, persistence.syncRoom(roomId, data.before))
      send(sender, result)
  }

  def leave(notifyHim: Boolean, who: ActorRef): Unit = {
    members.get(who).foreach {
      user =>
        members -= who
        context unwatch who
        if (notifyHim)
          who ! Room.RoomLeft(roomId)
        if (!members.values.exists(_ == user))
          publish(Responses.leftRoom(roomId, user, DateTime.now()))
        if (members.isEmpty)
          context stop self
    }
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