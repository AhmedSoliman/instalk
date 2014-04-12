package im.instalk.global

import play.api._
import akka.actor._
import java.util.concurrent.atomic.AtomicReference

object Instalk extends GlobalSettings {
  import im.instalk.actors.RoomManager

  private[this] val _actorSystem = new AtomicReference[ActorSystem]()
  private[this] val _roomManager = new AtomicReference[ActorRef]()

  def actorSystem(): ActorSystem = _actorSystem.get()
  def roomManager(): ActorRef = _roomManager.get()

  override def onStart(app: Application): Unit = {
    _actorSystem.set(ActorSystem("instalk", app.configuration.underlying.getConfig("instalk")))
    _roomManager.set(actorSystem.actorOf(Props[RoomManager], "rooms"))

  }

  override def onStop(app: Application): Unit = {
    actorSystem.shutdown()
    Logger.info("Waiting Instalk ActorSystem to Terminate")

    actorSystem.awaitTermination()
    Logger.info("Instalk ActorSystem Terminated...")
  }
}