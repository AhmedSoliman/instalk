package im.instalk

import play.api.libs.json._
import play.api.libs.functional.syntax._
import org.joda.time.DateTime

package object protocol {
  val VERSION = "0.1"
  type Version = String
  type SessionId = String
  type RoomId = String

  sealed trait OperationRequest {
    def r: RoomId
  }

  case class Join(r: RoomId) extends OperationRequest
  implicit val joinReads = Json.reads[Join]

  implicit val operation: Reads[OperationRequest] = new Reads[OperationRequest] {
    def reads(o: JsValue): JsResult[OperationRequest] = {
      (o \ "o").asOpt[String] match {
        case Some("join") =>
          joinReads.reads(o)
        case Some(_) =>
          JsError("operation.unknown")
        case None =>
          JsError("operation.missing")
      }
    }
  }

  object Validators {
    val version: Reads[Version] = (__ \ 'v).read[String]
    val heartbeat: Reads[Int] = (__ \ "heart-beat").read[Int]
  }

  object Responses {
    def Welcome(user: User) = Json.obj("welcome" -> 1, "user" -> user)
    val Timeout = Json.obj("timeout" -> 1)
    def HeartbeatAck(i: Int) = Json.obj("heart-beat-ack" -> i)
    def notSupportedOp(op: String) = Json.obj("o" -> op) ++ Errors.unsupportedOp

    def roomWelcome(roomId: RoomId, members: Iterable[User]) = Json.obj(
      "r" -> roomId,
      "o" -> "room-welcome",
      "data" -> Json.obj(
        "members" -> Json.toJson(members)
      )
    )

    def joinedRoom(roomId: RoomId, who: User, when: DateTime) = Json.obj(
      "r" -> roomId,
      "o" -> "joined",
      "data" -> Json.obj(
        "user" -> who,
        "when" -> when
      )
    )

    def leftRoom(roomId: RoomId, who: User, when: DateTime) = Json.obj(
      "r" -> roomId,
      "o" -> "left",
      "data" -> Json.obj(
        "user" -> who.username,
        "when" -> when
      )
    )

  }

  object Errors {
    private[this] def formatError(code: Int, msg: String) = Json.obj("error" -> Json.obj("code" -> code, "msg" -> msg))

    val InvalidVersion = formatError(1, "version.invalid")
    val unsupportedOp = formatError(2, "operation.unsupported")
    val missingOperation = formatError(3, "operation.missing")
    val notImplemented = formatError(999, "implementation.missing")
    def invalidOperationMessage(e: JsError) = formatError(4, JsError.toFlatJson(e).toString) //TODO: Fix me to proper error message
  }

}