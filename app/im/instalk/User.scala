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
package im.instalk

import scala.util.Random
import play.api.libs.functional.syntax._
import play.api.libs.json._

sealed trait User {
  def username: String
  def info: UserInfo
}

sealed trait UserInfo {
  def name: String
}

case class AnonymousUserInfo(val name: String, color: String) extends UserInfo

case class AuthenticatedUserInfo(val name: String, gravatar: String) extends UserInfo

case class AuthenticatedUser(val username: String, val info: AuthenticatedUserInfo) extends User

case class AnonymousUser(val username: String, val info: AnonymousUserInfo) extends User

object User {
  def generateName(s: String): String = s.replace('-', ' ')

  implicit val userFormat: Format[User] = new Format[User] {
    implicit val anonFmt = Json.format[AnonymousUserInfo]
    implicit val authFmt = Json.format[AuthenticatedUserInfo]

    def writes(o: User): JsValue = o match {
      case u: AnonymousUser =>
        Json.obj(
          "username" -> u.username,
          "auth" -> false,
          "info" -> Json.toJson(u.info)
        )
      case u: AuthenticatedUser =>
        Json.obj(
          "username" -> u.username,
          "auth" -> true,
          "info" -> Json.toJson(u.info)
        )
    }


    def reads(o: JsValue): JsResult[User] = {
      (o \ "auth").asOpt[Boolean] match {
        case Some(v) if v =>
          //AuthenticatedUser
          val reader =
            ((__ \ "username").read[String] and
                (__ \ "info").read[AuthenticatedUserInfo]
              )(AuthenticatedUser.apply _)
          reader.reads(o)
        case Some(_) | None =>
          //Anonymous
          val reader =
            ((__ \ "username").read[String] and
              (__ \ "info").read[AnonymousUserInfo]
              )(AnonymousUser.apply _)
          reader.reads(o)
      }
    }

  }


  private[this] val colorStr = "ABCDEF0123456789"

  def generateUsername: String = "Guest-" + Random.nextInt(5000)

  def generateColor: String = "#" + (1 to 6).map(_ => colorStr.charAt(Random.nextInt(16))).mkString("")
}
