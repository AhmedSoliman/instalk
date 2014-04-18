package im.instalk.actors

import akka.actor._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.json._
import im.instalk.protocol._
import scala.concurrent.duration._
import java.util.concurrent.atomic.AtomicReference

object WebSocketActor {
  case object GetActuator
  case class Actuator(act: (Iteratee[JsValue, Unit], Enumerator[JsValue]))
  case object Bored
  case class Send(msg: JsObject)
  case class Receive(msg: JsObject)

}

class WebSocketActor(r: RequestHeader, clientProps: (RequestHeader, ActorRef) => Props) extends Actor with ActorLogging {
  import WebSocketActor._
  implicit val ec = context.dispatcher


  private[this] val (enum, channel) = Concurrent.broadcast[JsValue]

  private[this] val iteratee = Iteratee.foreach[JsValue] { msg =>
    self ! Receive(msg.as[JsObject]) //hidden assumption that we can translate every message to JsObject
  }.map(_ => context.stop(self)) //when iteratee is done

  protected val client = context.actorOf(clientProps(r, self))

  log.info("WebSocketActor created for client at ({})", r.remoteAddress)

  private[this] val terminationGun = context.system.scheduler.scheduleOnce(10.seconds, self, Bored)

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
            send(Responses.Welcome)
            //context.setReceiveTimeout(30.seconds) //TODO: ENABLE HEART-BEAT
            context.become(initialized)
          }
        case e: JsError =>
          send(JsError.toFlatJson(e))
      }
    case Bored =>
     //terminate the socket, send a message
    send(Responses.Timeout)
      log.info("Client Timeout (BORED) –– (uninitialized) at {}", r.remoteAddress)
      context.stop(self)

  }

  def initialized: Receive = {
    case Receive(msg) =>
      // handle HEART-BEAT
      msg.validate(Validators.heartbeat) match {
        case JsSuccess(i, _) =>
          send(Responses.HeartbeatAck(i))
        case _ =>
          client ! Client.Request(msg)
      }

    case Send(msg) =>
      send(msg)

    case ReceiveTimeout =>
      //terminate the socket, send a message
      send(Responses.Timeout)
      log.info("Client Timeout (dying) at {}", r.remoteAddress)
      context.stop(self)

  }
  def receive = uninitialized

  def send(msg: JsObject): Unit = channel.push(msg)

  override def postStop = {
    channel.eofAndEnd()
    log.info("Client disconnected at {}", r.remoteAddress)
    super.postStop
  }
}