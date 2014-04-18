package im.instalk.actors

import akka.actor._
import play.api.mvc._
import play.api.libs.json._


object Client {
  def props(r: RequestHeader, socket: ActorRef): Props = Props(new Client(r, socket))

  case class Response(msg: JsObject)
  case class Request(msg: JsObject)

}

class Client(r: RequestHeader, socket: ActorRef) extends Actor with ActorLogging {
  var rooms = Map.empty[String, ActorRef]

  def receive = {
    case Client.Request(msg) =>
      //handle
    case Client.Response(msg) =>
      socket ! WebSocketActor.Send(msg)
  }
}