package im.instalk.actors

import akka.actor._
import play.api.mvc._
import play.api.libs.json._


object Client {
  trait State
  case object New extends State
  case object Anonymous extends State
  case object Authenticated extends State


  trait Data
  case class Rooms(rooms: Map[String, ActorRef])

  case class Request(msg: JsObject)

}

class Client(r: Headers) extends FSM[Client.State, Client.Data] {

}