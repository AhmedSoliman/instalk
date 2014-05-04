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
import play.api.libs.json._
import im.instalk.{AnonymousUserInfo, AnonymousUser, User}
import im.instalk.global.Instalk
import im.instalk.protocol._
import im.instalk.actors.RoomManager.JoinOrCreate
import im.instalk.actors.Room.UpdateUser


object Client {
  def props(user: User, socket: ActorRef): Props = Props(new Client(user, socket))

  case class Response(msg: JsObject)

  case class Request(msg: JsObject)

}


class Client(user: User, socket: ActorRef) extends Actor with ActorLogging {
  //see if we know this user before or not
  val roomManager = Instalk.roomManager()
  var _user = user
  var rooms = Map.empty[String, ActorRef]


  def receive = {
    case Client.Request(msg) =>
      handleRequest(msg)
    case Client.Response(msg) =>
      send(msg)
    case UpdateUser(newUser) =>
      _user = newUser
    case Room.RoomJoined(roomId, members, lastMessages) =>
      context watch sender
      rooms += (roomId -> sender)
      send(Responses.roomWelcome(roomId, members, lastMessages))
    case Room.RoomLeft(roomId) =>
      rooms -= roomId
      context unwatch sender
      send(Responses.roomBye(roomId))
    case Terminated(actor) =>
      rooms.find(_._2 == actor).map {
        case (name, ref) =>
          context unwatch ref
          rooms -= name
      }.getOrElse(log.warning("Actor received DEATH for actor {} while is not a room we are members in!", actor))
  }

  def send(msg: JsObject): Unit =
    socket ! WebSocketActor.Send(msg)

  def routeToRoom(o: OperationRequest) =
    rooms.get(o.r) match {
      case Some(room) =>
        room ! o
      case None =>
        send(Errors.unknownRoom)
    }

  def handleRequest(msg: JsObject): Unit = {
    msg.validate[OperationRequest] match {
      case JsSuccess(o: Join, _) =>
        roomManager ! JoinOrCreate(o.r, _user)
      case JsSuccess(o: Leave, _) =>
        routeToRoom(o)
      case JsSuccess(o: SetUserInfoRequest, _) =>
        //apply changes to the local User object and propagate to all rooms
        val newUser = applyUserInfo(o.data)
        if (newUser != _user) {
          _user = newUser
          //propagate
          rooms.foreach(_._2 ! UpdateUser(_user))
        }
      case JsSuccess(o: RoomOp, _) =>
        routeToRoom(o)
      case JsSuccess(o, _) =>
        //something not implemented yet
        send(Errors.notImplemented)
      case e: JsError =>
        send(Errors.invalidOperationMessage(e))
    }
  }

  def applyUserInfo(changes: AnonymousInfoModification): User = {
    _user match {
      case u: AnonymousUser =>
        (changes.name, changes.color) match {
          case (Some(name), Some(color)) =>
            u.copy(info = AnonymousUserInfo(name, color))
          case (Some(name), None) =>
            u.copy(info = u.info.copy(name = name))
          case (None, Some(color)) =>
            u.copy(info = u.info.copy(color = color))
          case (None, None) =>
            _user
        }
      case _ =>
        //this should not be possible
        ???
    }
  }
}