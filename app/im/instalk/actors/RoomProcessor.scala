package im.instalk.actors

import akka.persistence._
import akka.actor._
import play.api.libs.json.JsValue

class RoomProcessor(room: String) extends Processor with ActorLogging{

  //override def processorId = room
  case object RecoverRequest
  def receive = {
    case Persistent(payload: JsValue, sequenceNr) =>
    // message successfully written to journal
      log.info("I got a persistent message ({}) with sequence ({})", payload, sequenceNr)
      //sender() ! WebsocketHandler.MessageOut(payload)

    case PersistenceFailure(payload, sequenceNr, cause) =>
    // message failed to be written to journal
      log.warning("Processor could not persist the message to the journal, rejecting")
    case other: JsValue =>
      other.validate
    case other =>
      log.info("A message that should be unhandled")
  }
}
