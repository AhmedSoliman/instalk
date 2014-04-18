package im.instalk.global

import play.api._
import akka.actor._
import java.util.concurrent.atomic.AtomicReference
import im.instalk.actors.ClientManager
import com.redis._

object Instalk extends GlobalSettings {

  private[this] val _actorSystem = new AtomicReference[ActorSystem]()
  private[this] val _clientManager = new AtomicReference[ActorRef]()
  private[this] val _redis = new AtomicReference[RedisClientPool]()

  def actorSystem(): ActorSystem = _actorSystem.get()
  def clientManager(): ActorRef = _clientManager.get()
  def redis(): RedisClientPool = _redis.get()

  override def onStart(app: Application): Unit = {
    //connect to redis
    val redisHost = app.configuration.getString("instalk.redis.host").getOrElse("localhost")
    val redisPort = app.configuration.getInt("instalk.redis.port").getOrElse(6379)
    val redisDb = app.configuration.getInt("instalk.redis.db").getOrElse(0)

    _redis.set(new RedisClientPool(redisHost, redisPort, database = redisDb))
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