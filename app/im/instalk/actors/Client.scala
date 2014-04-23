package im.instalk.actors

import akka.actor._
import play.api.libs.json._
import im.instalk.User
import im.instalk.global.Instalk
import im.instalk.protocol._
import im.instalk.actors.RoomManager.JoinOrCreate


object Client {
  def props(user: User, socket: ActorRef): Props = Props(new Client(user, socket))

  case class Response(msg: JsObject)

  case class Request(msg: JsObject)

}


class Client(user: User, socket: ActorRef) extends Actor with ActorLogging {
  //see if we know this user before or not
  val roomManager = Instalk.roomManager()
  var _user = user
  var rooms = Map.empty[String, ActorRef]


  def receive = {
    case Client.Request(msg) =>
      handleRequest(msg)
    case Client.Response(msg) =>
      send(msg)
  }

  def send(msg: JsObject): Unit =
    socket ! WebSocketActor.Send(msg)

  def handleRequest(msg: JsObject): Unit = {
    msg.validate[OperationRequest] match {
      case JsSuccess(o: Join, _) =>
        roomManager ! JoinOrCreate(o.r, _user)
      case JsSuccess(o, _) =>
        //something not implemented yet
        send(Errors.notImplemented)
      case e: JsError =>
        send(Errors.invalidOperationMessage(e))
    }
  }
}