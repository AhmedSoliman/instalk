package im.instalk.actors

import akka.actor._

object RoomManager {
  case class FindOrCreate(room: String)

  case class Room(actor: ActorRef)
}
class RoomManager extends Actor with ActorLogging{
  log.info("Staring the Room MANAGER")
  def receive = {
    case RoomManager.FindOrCreate(room) =>
      //local search
      sender ! (RoomManager.Room(context.child(room).getOrElse {
        context.actorOf(Props(new RoomProcessor(room)), name=room)
      }))
  }
}
