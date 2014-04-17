package im.instalk.global

import play.api._
import akka.actor._
import java.util.concurrent.atomic.AtomicReference
import im.instalk.actors.ClientManager

object Instalk extends GlobalSettings {

  private[this] val _actorSystem = new AtomicReference[ActorSystem]()
  private[this] val _clientManager = new AtomicReference[ActorRef]()

  def actorSystem(): ActorSystem = _actorSystem.get()
  def clientManager(): ActorRef = _clientManager.get()

  override def onStart(app: Application): Unit = {
    _actorSystem.set(ActorSystem("instalk", app.configuration.underlying.getConfig("instalk")))
    _clientManager.set(actorSystem.actorOf(Props[ClientManager], "clients"))

  }

  override def onStop(app: Application): Unit = {
    actorSystem.shutdown()
    Logger.info("Waiting Instalk ActorSystem to Terminate")

    actorSystem.awaitTermination()
    Logger.info("Instalk ActorSystem Terminated...")
  }
}