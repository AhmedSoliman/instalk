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
package im.instalk.test.integration

import im.instalk.test.util._
import im.instalk.test.util.WebSocketClient.Messages._
import im.instalk.User
import org.scalatest._
import akka.testkit._
import akka.actor._
import play.api.libs.json._
import scala.concurrent.duration._
import im.instalk.protocol.Message

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class RoomSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with FlatSpecLike
with Matchers with PlayServer with Helpers {

  def this() = this(ActorSystem("RoomSpecSystem"))

  "Rooms" should "should be created if I'm trying to join" in {
    withSocket(false) {
      (socket, probe) =>
        initialise(socket, probe)
        val jr = joinRoom("Soliman", socket, probe)
        jr._1 should have size (0)
    }
  }
  it should "inform others that I joined the room" in {
    val room = "Soliman"
    //delete the history first
    import im.instalk.global.Instalk
    Instalk.persistence().dropRoomHistory(room)

    withSocket(true) {
      (client1, probe1) =>
        val user1 = initialise(client1, probe1)
        withSocket(true) {
          (client2, probe2) =>
            val user2 = initialise(client2, probe2)
            withSocket(true) {
              (client3, probe3) =>

              /** actual Implementation **/
                val user3 = initialise(client3, probe3)
                joinRoom(room, client3, probe3)._1 should have size (0) //client 3 created the room
                val u2 = joinRoom(room, client2, probe2) //client 2 joined the room
                u2._1 should have size (1)
                u2._1.head should equal(user3)
                matchJoined(toJson(probe3.expectMsgType[TextMessage].text), room, user2) //client 3 should get a notification about client 2

                val u3 = joinRoom(room, client1, probe1) //client 1 joined the room
                u3._1 should have size (2)

                matchJoined(toJson(probe3.expectMsgType[TextMessage].text), room, user1) //client 3 should get a notification about client 1
                matchJoined(toJson(probe2.expectMsgType[TextMessage].text), room, user1) //client 2 should get a notification about client 1
                //now total 3 are in the room. let's send a message
                val msg1 = Message("Hello World")
                val msg2 = Message("Nice Work")
                sendMessage(room, client1, msg1) //client 1 sent message
                Thread.sleep(500)
                sendMessage(room, client2, msg2) // client 2 sent message

                receiveMessage(room, probe3)._3 should equal(msg1)
                receiveMessage(room, probe3)._3 should equal(msg2)

                receiveMessage(room, probe2)._3 should equal(msg1)
                receiveMessage(room, probe2)._3 should equal(msg2)

                val (u1, seq1, m1) = receiveMessage(room, probe1)
                m1 should equal(msg1)
                seq1 should equal(0)
                u1 should equal(user1)
                val (uu2, seq2, m2) = receiveMessage(room, probe1)
                m2 should equal(msg2)
                seq2 should equal(1)
                uu2 should equal(user2)

                leaveRoom(room, client1, probe1)
                receiveEvent(room, "left", probe2) should equal(user1)
                receiveEvent(room, "left", probe3) should equal(user1)


            }
        }
    }
  }
  it should "return latest messages when I join the room" in {
    val room = "Montaro"
    //delete the history first
    import im.instalk.global.Instalk
    Instalk.persistence().dropRoomHistory(room)

    val msg1 = Message("This is message no 1")
    val msg2 = Message("This is message no 2")
    val msg3 = Message("This is message no 3")
    val msg4 = Message("This is message no 4")

    withSocket(true) {
      (client1, probe1) =>
        val user1 = initialise(client1, probe1)
        //assert that messages list is empty in room creation
        val uj1 = joinRoom(room, client1, probe1)
        val latestMessages = uj1._2
        latestMessages.size should equal(0)
        //user 1 will send one message
        sendMessage(room, client1, msg1)
        withSocket(true) {
          (client2, probe2) =>
            val user2 = initialise(client2, probe2)
            //assert that there is one message sent by user 1
            val uj2 = joinRoom(room, client2, probe2)
            val latestMessages = uj2._2
            latestMessages.size should equal(1)
            (latestMessages.head \ "txt").as[String] should equal(msg1.txt)
            (latestMessages.head \ "sender" \ "username").as[String] should equal(user1.username)
            //user 2 will send 2 more messages, then user 1 will send 1 message
            sendMessage(room, client2, msg2)
            sendMessage(room, client2, msg3)
            sendMessage(room, client1, msg4)
            withSocket(true) {
              (client3, probe3) =>
              //assert that the room will sync the 4 history messages in correct order
                initialise(client3, probe3)
                val uj3 = joinRoom(room, client3, probe3)
                val latestMessages = uj3._2
                latestMessages.size should equal(4)
                (latestMessages(0) \ "txt").as[String] should equal(msg4.txt)
                (latestMessages(0) \ "sender" \ "username").as[String] should equal(user1.username)
                (latestMessages(1) \ "txt").as[String] should equal(msg3.txt)
                (latestMessages(1) \ "sender" \ "username").as[String] should equal(user2.username)
                (latestMessages(2) \ "txt").as[String] should equal(msg2.txt)
                (latestMessages(2) \ "sender" \ "username").as[String] should equal(user2.username)
                (latestMessages(3) \ "txt").as[String] should equal(msg1.txt)
                (latestMessages(3) \ "sender" \ "username").as[String] should equal(user1.username)
            }
        }
    }
  }
}
