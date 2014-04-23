package im.instalk.actors

import akka.actor._
import play.api.mvc._
import scala.util.Random
import im.instalk.User

object ClientManager {
  case class CreateClient(r: RequestHeader)
}
class ClientManager extends Actor with ActorLogging {
  def receive = {
    case ClientManager.CreateClient(r) =>
      //try to figure out who is the user
      //TODO: let's query the user registery, for now we are assuming anonymous
      val actor = context.actorOf(Props(new WebSocketActor(User.Anonymous(User.generateColor, User.generateUsername), Client.props, r.remoteAddress)), "client-" + Math.abs(Random.nextInt))
      actor.tell(WebSocketActor.GetActuator, sender)
  }
}