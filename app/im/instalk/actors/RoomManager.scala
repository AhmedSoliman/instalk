package im.instalk.actors

import akka.actor._
import im.instalk.protocol.{RoomId}
import im.instalk.User

object RoomManager {
  case class JoinOrCreate(r: RoomId, u: User)
}

class RoomManager extends Actor with ActorLogging {
  import RoomManager._
  //respond to sender, wrap in Client.Response()
  var rooms = Map.empty[RoomId, ActorRef]

  def receive = {
    case msg @ JoinOrCreate(r, u) =>
      //do we know that room?
      (rooms.get(r).getOrElse(createRoom(r))) forward msg
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
    val room = context.actorOf(Props(new Room(r)), name = r)
    rooms += (r -> room)
    context watch room
    room
  }

  override def unhandled(msg: Any) =
    log.error("GOT SOMETHING I DO NOT KNOW: {}", msg)
}