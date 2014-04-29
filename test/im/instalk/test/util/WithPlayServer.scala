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
package im.instalk.test.util

import play.api.test._
import play.api.test.FakeApplication
import org.scalatest._
import play.api.Logger
import java.net.URI
import im.instalk.test.util.WebSocketClient.Messages.{Disconnected, Disconnecting, Connected, Connecting}
import akka.testkit._

trait PlayServer extends BeforeAndAfterAll {
  this: TestKit with Suite with ImplicitSender =>

  val app: FakeApplication = FakeApplication(additionalConfiguration = Map("instalk.cassandra.table" -> "messagesTest"))

  val port = Helpers.testServerPort

  implicit def implicitApp = app

  implicit def implicitPort: Port = port

  val server = TestServer(port, app)

  def withSocket[T](ignoreMessagesOnClose: Boolean)(body: (WebSocketClient, TestProbe) => T): T = {
    val testProbe = TestProbe()
    val c = WebSocketClient(new URI(s"ws://localhost:$port/websocket"), testProbe.ref)
    c.connect
    testProbe.expectMsg(Connecting)
    testProbe.expectMsgType[Connected]
    val r = try {
      body(c, testProbe)
    } finally {
      c.disconnect
    }
    if (!ignoreMessagesOnClose) {
      testProbe.expectMsg(Disconnecting)
      testProbe.expectMsgType[Disconnected]
    }
    r
  }

  override def beforeAll(): Unit = {
    Logger.info("Starting the Play TEST Server")
    server.start()
  }

  override def afterAll(): Unit = {
    server.stop()
    Logger.info("Play TEST Server [STOPPED]")
  }

}