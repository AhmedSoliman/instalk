package im.instalk.actors

import akka.actor._
import im.instalk.User
import im.instalk.protocol._
import im.instalk.actors.RoomManager.JoinOrCreate
import play.api.libs.json._
import org.joda.time.DateTime

class Room(roomId: RoomId) extends Actor with ActorLogging {
  log.info("Room {} created", roomId)
  var members = Map.empty[ActorRef, User]

  def receive = {
    case JoinOrCreate(_, u) =>
      //send him welcome message
      send(sender, Responses.roomWelcome(roomId, members.values))
      publish(Responses.joinedRoom(roomId, u, DateTime.now()))
      //put the guy in the members
      members += (sender -> u)
      context watch sender
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

  def publish(msg: JsObject): Unit =
    members.foreach {
      case (k, v) => send(k, msg)
    }

  def send(to: ActorRef, msg: JsObject): Unit =
    to ! Client.Response(msg)

}