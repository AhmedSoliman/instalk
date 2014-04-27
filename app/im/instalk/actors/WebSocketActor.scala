/*
 * Copyright 2014 The Instalk Project
 *
 * The Instalk Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package im.instalk.actors

import akka.actor._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import im.instalk.protocol._
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicReference
import im.instalk.User
import java.util.concurrent.TimeUnit

object WebSocketActor {
  case object GetActuator
  case class Actuator(act: (Iteratee[String, Unit], Enumerator[String]))
  case object Bored
  case class Send(msg: JsObject)
  case class Receive(msg: JsObject)

}

class WebSocketActor(user: User, clientProps: (User, ActorRef) => Props, remoteAddress: String) extends Actor with ActorLogging {
  import WebSocketActor._
  import play.api.Play.current
  implicit val ec = context.dispatcher


  private[this] val (enum, channel) = Concurrent.broadcast[String]

  private[this] val iteratee = Iteratee.foreach[String] { msg =>
    try {
      self ! Receive(Json.parse(msg).as[JsObject])
    } catch {
      case e: Throwable =>
        send(Errors.badJson)
    }
  }.map(_ => context.stop(self)) //when iteratee is done

  protected val client = context.actorOf(clientProps(user, self))

  log.info("WebSocketActor created for client at ({})", remoteAddress)
  val idleTerminationPeriod: FiniteDuration = current.configuration.getMilliseconds("instalk.websocket.idle-terminate-grace-period").map(Duration(_, TimeUnit.MILLISECONDS)).getOrElse(10.seconds)
  private[this] val terminationGun = context.system.scheduler.scheduleOnce(idleTerminationPeriod, self, Bored)

  def uninitialized: Receive = {
    case GetActuator =>
      sender ! WebSocketActor.Actuator((iteratee, enum))

    case Receive(msg) =>
      msg.validate(Validators.version) match {
        case JsSuccess(version, _) =>
          //check if the version is correct
          if (version != VERSION)
            send(Errors.InvalidVersion)
          else {
            terminationGun.cancel()
            //let's send him a welcome message and become initialized
            send(Responses.welcome(user))
            //context.setReceiveTimeout(30.seconds) //TODO: ENABLE HEART-BEAT
            context.become(initialized)
          }
        case e: JsError =>
          send(JsError.toFlatJson(e))
      }
    case Bored =>
     //terminate the socket, send a message
    send(Responses.timeout)
      log.info("Client Timeout (BORED) –– (uninitialized) at {}", remoteAddress)
      context.stop(self)

  }

  def initialized: Receive = {
    case Receive(msg) =>
      // handle HEART-BEAT
      msg.validate(Validators.heartbeat) match {
        case JsSuccess(i, _) =>
          send(Responses.heartbeatAck(i))
        case _ =>
          client ! Client.Request(msg)
      }

    case Send(msg) =>
      send(msg)

    case ReceiveTimeout =>
      //terminate the socket, send a message
      send(Responses.timeout)
      log.info("Client Timeout (dying) at {}", remoteAddress)
      context.stop(self)

  }
  def receive = uninitialized

  def send(msg: JsObject): Unit = channel.push(msg.toString())

  override def postStop = {
    channel.eofAndEnd()
    log.info("Client disconnected at {}", remoteAddress)
    super.postStop
  }
}