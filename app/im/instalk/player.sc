import im.instalk.protocol._
import im.instalk.User.Anonymous
import org.joda.time.DateTime
Responses.welcome(Anonymous("#55BBAF", "Ahmed Soliman"))
Responses.roomWelcome("MyRoomId", List(Anonymous("#55BBAF", "Ahmed Soliman")))
Responses.leftRoom("MyRoomId", Anonymous("#55BBAF", "Ahmed Soliman"), DateTime.now())

Responses.roomMessage(RoomMessage("MyRoomId", Anonymous("#55BBAF", "Ahmed Soliman") , SeqEnvelope(1, Message("Hello World"))))