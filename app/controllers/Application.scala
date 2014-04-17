package controllers

import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.Future
import im.instalk.global.Instalk
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import im.instalk.actors.{ ClientManager, WebSocketActor}

object Application extends Controller {

  import play.api.Play.current
  val globals = current.global.asInstanceOf[Instalk.type]
  implicit val ec = globals.actorSystem.dispatcher
  implicit val to = Timeout(10.seconds)
  def index = Action {
    Ok("Hello World")
  }

  def websocket = WebSocket.async[JsValue] {
    request =>
      val act = (globals.clientManager ? ClientManager.CreateClient(request)).mapTo[WebSocketActor.Actuator]
      act.map(x => x.act)
  }
}