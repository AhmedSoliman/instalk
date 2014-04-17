package im.instalk.actors

import akka.actor._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._

object WebSocketActor {
  case object GetActuator
  case class Actuator(act: (Iteratee[JsValue, Unit], Enumerator[JsValue]))
  case class Send(msg: JsObject)
  case class Receive(msg: JsObject)

}

class WebSocketActor(r: RequestHeader) extends Actor with ActorLogging {
  implicit val ec = context.dispatcher

  val (enum, channel) = Concurrent.broadcast[JsValue]

  val iteratee = Iteratee.foreach[JsValue] { msg =>
    self ! WebSocketActor.Receive(msg.as[JsObject]) //hidden assumption that we can translate every message to JsObject
  }.map(_ => context.stop(self)) //when iteratee is done

  log.info("WebSocketActor created")

  def receive = {
    case WebSocketActor.GetActuator =>
      sender ! WebSocketActor.Actuator((iteratee, enum))

    case WebSocketActor.Send(msg) =>
      //flushing the message to the user
      channel.push(msg)
    case WebSocketActor.Receive(msg) =>
      //got a message from the user
    log.info("Got message:" + msg)
    self ! WebSocketActor.Send(msg ++ Json.obj("ack" -> true))
  }

  override def postStop = {
    log.info("Client disconnected")
    super.postStop
  }
}