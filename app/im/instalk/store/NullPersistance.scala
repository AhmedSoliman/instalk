package im.instalk.store

import im.instalk.protocol.RoomId
import play.api.libs.json.JsObject
import im.instalk.User

class NullPersistance extends Persistence {
  override def storeEvent(room: RoomId, seqNr: Long, op: String, data: JsObject, topic: String, members: Iterable[User]): Unit = ()

  override def setTopic(room: RoomId, topic: String): Unit = ()

  override def dropAllRooms(): Unit = ()

  override def syncRoom(room: RoomId, seqNr: Long): Iterable[JsObject] = Iterable.empty[JsObject]

  override def close(): Unit = ()

  override def dropRoomHistory(room: RoomId): Unit = ()

  override def getNextAvailableSeqNr(room: RoomId): Option[Long] = None

  override def getLatestMessages(room: RoomId): Iterable[JsObject] = Iterable.empty[JsObject]

  override def getTopic(room: RoomId): Option[String] = None
}