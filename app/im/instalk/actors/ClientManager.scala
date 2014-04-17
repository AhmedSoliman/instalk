package im.instalk.actors

import akka.actor._
import play.api.mvc._
import scala.util.Random

object ClientManager {
  case class CreateClient(r: RequestHeader)
}
class ClientManager extends Actor with ActorLogging {
  def receive = {
    case ClientManager.CreateClient(r) =>
      val actor = context.actorOf(Props(new WebSocketActor(r)), "client-" + Math.abs(Random.nextInt))
      actor.tell(WebSocketActor.GetActuator, sender)
  }
}