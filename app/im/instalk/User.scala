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
import play.api.libs.json._

sealed trait User {
  def color: String
  def username: String
  def verified: Boolean
  def link: Option[String]
}

object User {
  case class Anonymous(val color: String, val username: String) extends User {
    val verified = false
    val link = None
  }

  implicit val userFormat: Format[User] = new Format[User] {
    val anonFmt = Json.format[Anonymous]

    def writes(o: User): JsValue = {
      o match {
        case anon: Anonymous =>
          //do something
          anonFmt.writes(anon)
        case _ => //do something else
          Json.obj() //TODO: Fix when implementing authenticated user
      }
    }
    def reads(o: JsValue): JsResult[User] = {
      val v = (o \ "verified").asOpt[Boolean]
      v.map (
        verified =>
          if (! verified) {
            anonFmt.reads(o)
          } else {
            //the guy is authenticated, not implemented yet
            ???
          }
      ).getOrElse(anonFmt.reads(o))
    }

  }


  private[this] val colorStr = "ABCDEF0123456789"

  def generateUsername: String = "Anonymous-" + Random.nextInt(5000)
  def generateColor: String = "#" + (1 to 6).map(_ => colorStr.charAt(Random.nextInt(16))).mkString("")
}
