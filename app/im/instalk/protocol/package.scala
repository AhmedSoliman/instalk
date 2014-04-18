package im.instalk

import play.api.libs.json._
import play.api.libs.functional.syntax._

package object protocol {
  val VERSION = "0.1"
  type Version = String
  type SessionId = String

  object Validators {
    val version: Reads[Version] = (__ \ 'v).read[String]
    val heartbeat: Reads[Int] = (__ \ "heart-beat").read[Int]

  }

  object Responses {
    val Welcome = Json.obj("welcome" -> 1)
    val Timeout = Json.obj("timeout" -> 1)
    def HeartbeatAck(i: Int) = Json.obj("heart-beat-ack" -> i)

  }

  object Errors {
    val InvalidVersion = Json.obj("error" -> Json.obj("code" -> 1, "msg" -> "version.invalid"))
  }

}