package im.instalk.actors

import akka.actor._
import akka.persistence._
import play.api.libs.json._
import im.instalk.global.Instalk
import play.api.mvc._


object WebsocketHandler {
  def props(room: String): ActorRef => Props = (out: ActorRef) => Props(new WebsocketHandler(out, room))
  case class MessageOut(js: JsValue)
}

class WebsocketHandler(out: ActorRef, room: String) extends Actor with Stash with ActorLogging {
  log.debug("Starting Websocket Actor for room({})", room)
  log.debug("Finding room processor")

  override def preStart(): Unit = {
    Instalk.roomManager().tell(RoomManager.FindOrCreate(room), self)
  }

  def receive = roomNotReady

  def roomNotReady: Receive = {
    case RoomManager.Room(roomActor) =>
      log.info("Room Processor found for room({})", room)
      context.become(roomReady(roomActor))
      unstashAll()
    case _ =>
      stash()
  }

  def roomReady(roomActor: ActorRef): Receive = {
   case msg1:JsValue =>
     //incoming from client
     log.debug("Sending the message to the processor")
     roomActor ! Persistent(msg1)
    case WebsocketHandler.MessageOut(msg2) =>
     out ! msg2
  }

 def unhandled: Receive = {
   case _ => log.error("Got an unknown type on the websocket handler")
 }

  override def postStop() = {
    log.warning("Lost websocket connection bro")
  }
}