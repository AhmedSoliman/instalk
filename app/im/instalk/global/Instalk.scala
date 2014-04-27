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
package im.instalk.global

import play.api._
import akka.actor._
import java.util.concurrent.atomic.AtomicReference
import im.instalk.actors.{ ClientManager, RoomManager }
import com.redis._

object Instalk extends GlobalSettings {

  private[this] val _actorSystem = new AtomicReference[ActorSystem]()
  private[this] val _clientManager = new AtomicReference[ActorRef]()
  private[this] val _roomManager = new AtomicReference[ActorRef]()
  private[this] val _redis = new AtomicReference[RedisClientPool]()

  def actorSystem(): ActorSystem = _actorSystem.get()
  def clientManager(): ActorRef = _clientManager.get()
  def roomManager(): ActorRef = _roomManager.get()
  def redis(): RedisClientPool = _redis.get()

  override def onStart(app: Application): Unit = {
    //connect to redis
    val redisHost = app.configuration.getString("instalk.redis.host").getOrElse("localhost")
    val redisPort = app.configuration.getInt("instalk.redis.port").getOrElse(6379)
    val redisDb = app.configuration.getInt("instalk.redis.db").getOrElse(0)

    _redis.set(new RedisClientPool(redisHost, redisPort, database = redisDb))
    _actorSystem.set(ActorSystem("instalk", app.configuration.underlying.getConfig("instalk")))
    _clientManager.set(actorSystem.actorOf(Props[ClientManager], "clients"))
    _roomManager.set(actorSystem.actorOf(Props[RoomManager], "rooms"))

    Logger.info(
      """
        |.___                 __         .__   __
        ||   | ____   _______/  |______  |  | |  | __
        ||   |/    \ /  ___/\   __\__  \ |  | |  |/ /
        ||   |   |  \\___ \  |  |  / __ \|  |_|    <
        ||___|___|  /____  > |__| (____  /____/__|_ \
        |         \/     \/            \/          \/
      """.stripMargin)
  }

  override def onStop(app: Application): Unit = {
    actorSystem.shutdown()
    Logger.info("Waiting Instalk ActorSystem to Terminate")

    actorSystem.awaitTermination()
    Logger.info("Instalk ActorSystem Terminated...")
  }
}