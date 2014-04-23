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
      ).getOrElse(JsError("verified key is not defined at user object"))
    }

  }


  private[this] val colorStr = "ABCDEF0123456789"

  def generateUsername: String = "Anonymous-" + Random.nextInt(5000)
  def generateColor: String = "#" + (1 to 6).map(_ => colorStr.charAt(Random.nextInt(16))).mkString("")
}
