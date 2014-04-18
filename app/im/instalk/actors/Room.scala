package im.instalk.actors

import akka.actor._

class Room(roomId: String) extends Actor with ActorLogging {
  def receive = Actor.emptyBehavior
}