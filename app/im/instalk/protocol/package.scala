package im.instalk

import play.api.libs.json._
import play.api.libs.functional.syntax._

package object protocol {
  type Version = String
  type SessionId = String

  object Validators {
    val version: Reads[Version] = (__ \ 'v).read[String]

  }

  object Responses {
    def sessionCreated(version: Version, sessionId: SessionId): JsObject =
      Json.obj("version" -> version, "s" -> sessionId)
  }

  object Errors {

  }

}