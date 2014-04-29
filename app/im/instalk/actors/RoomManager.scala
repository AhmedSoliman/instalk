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
import im.instalk.protocol.{RoomId}
import im.instalk.User
import im.instalk.global.Instalk
import com.codahale.metrics.{Gauge, MetricRegistry}
import im.instalk.store.Persistence

object RoomManager {
  case class JoinOrCreate(r: RoomId, u: User)
}

class RoomManager(persistence: Persistence) extends Actor with ActorLogging {
  import RoomManager._
  //respond to sender, wrap in Client.Response()
  var rooms = Map.empty[RoomId, ActorRef]

  val roomsMeter = Instalk.metrics.meter(MetricRegistry.name(classOf[RoomManager], "rooms.created"))
  val liveRooms = Instalk.metrics.register(MetricRegistry.name(classOf[RoomManager], "rooms.alive"),
    new Gauge[Int] {
      override def getValue(): Int = rooms.size
    })

  def receive = {
    case msg @ JoinOrCreate(r, u) =>
      //do we know that room?
      val room = (rooms.get(r).getOrElse(createRoom(r)))
      room forward msg
    case Terminated(room) =>
      //room was terminated, remove from the map, advertise on log
      val result = rooms.find { case (r, a) => a == room }
      result.foreach {
        case (r, a) =>
          rooms -= r
          log.info("Room {} was terminated, probably all members left", r)
      }
  }

  def createRoom(r: RoomId): ActorRef = {
    //TODO: some crazy distribution algorithm goes here
    val room = context.actorOf(Props(new Room(r, persistence)), name = r)
    roomsMeter.mark()
    rooms += (r -> room)
    context watch room
    room
  }

  override def unhandled(msg: Any) =
    log.error("GOT SOMETHING I DO NOT KNOW: {}", msg)
}