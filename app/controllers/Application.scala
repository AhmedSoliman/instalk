package controllers

import play.api.mvc._
import play.api.libs.json._
import im.instalk.actors.WebsocketHandler
import scala.concurrent.Future

object Application extends Controller {

  import play.api.Play.current

  def index = Action {
    Ok("Hello World")
  }

 def websocket(room: String) = WebSocket.tryAcceptWithActor[JsValue, JsValue] {
   request =>
     val domain = current.configuration.getString("application.domain").getOrElse("unknown.com")
     println("Request Host is:" + domain)
     (Future.successful {
         val origin = request.headers.get("Origin").getOrElse("")
         println("Requesting Origin is:" + origin)
         if (origin == domain)
           Right(WebsocketHandler.props(room))
           else Left(Forbidden)
       }): Future[Either[Result, WebSocket.HandlerProps]]
 }
}