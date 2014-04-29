# Instalk Protocol Definition
This is the standard protocol definition of Instalk, the protocol is designed to be lightweight and easy to implement clients for while giving some space for expansion in the future.

The protocol is entirely JSON and uses WebSocket as the default transport.

### The EndPoint
When you establish a connection to the server (websocket), this is the end point URL that you need to use (replace ws with wss if you want TLS):

```
ws://127.0.0.0:4040/websocket
```
## Initialisation
Once you are connected to the WebSocket server you need to identify yourself within a grace period of 10 seconds (based on configuration key `instalk.websocket.idle-terminate-grace-period`)

The client have to send the initialisation message with the protocol version the client is using

```
{"v": "0.1"}
```
If all went well with the server, the server will send you the welcome message

```
{"welcome":1,"user":{"color":"#55BBAF","username":"Anonymous-6215"}}
```
If you didn't initialise within the `idle-terminate-grace-period` the server will disconnect your socket after sending you `{"timeout": 1}`

In case of an error you will be receiving an error message according to the [Handling Errors]() specifications.
The `user` object is generated unless you identify yourself, the color attribute is a stable HEX color that clients should use to represent this user, if the user is identified we will be using his gravatar picture for that.

## Joining and Leaving Rooms
Once you connect and initialise your connection properly, you are at the lobby, there is no room that you are member of, you need to either create or join a room to start instalking. Creation and Joining to Instalk is a transparent operation, if you are joining a room that does not exist, it will be automatically be created for you.

If you want to join a room, you start by sending a room identifier in your join request. Clients can generate random Room ID if a room is not provided in the URL.

```
{"r": "MyRoomID", "o": join}
```
You should expect to get a welcome message like

```
{"r":"MyRoomID","o":"room-welcome","data":{"members":[]}}
```
The `data/members` attribute include all the members of this room, it's an empty list meaning that it's a freshly created room. If there are members in this room already, you will be seeing something like

```
{
  "r": "MyRoomId", "o": "room-welcome", 
  "data": {
      "members": [{"color": "#55BBAF","username": "Ahmed Soliman"}]
   }
}
```
Others in the room will receive an `Event` that you have joined to the room

```
{"r": "MyRoomId", "o": "join", 
  "data": {
    "user": {"color": "#55BBAF","username": "Ahmed Soliman"}, 
    "when": 1398592566109
  }
}
```

If you are planning to purposely leave a room, you need to send a leave request (note that closing your websocket connection automatically does that to all your rooms)

```
{"r": "MyRoomId", "o": "leave"}

```
As a response, you will receive

```
{"r":"MyRoomId","o":"room-bye"}
```

Other members of the room will receive an `Event` that you left, they will be receiving this message in your case

```
{"r": "MyRoomId", "o": "left", "data": 
  {
   "user": {"color": "#55BBAF","username": "Ahmed Soliman"}, 
   "when": 1398592566109
  }
}
```

## Sending Messages To Room
Once you are a member of a room you can start sending messages to the users of the room by sending requests like this

```
{"r": "MyRoomId", "o": "msg", "data":
  {"message": {"txt": "Hello World"}}
}
```
The server then will add a sequential `#` number for your message and distribute your message to everybody in the room _including yourself_ after adding more data to it, your message in this case will be

```
{"r": "MyRoomId", "o": "msg", "data":
  {
   "msg": {
     "#": 1,
     "sender": {"color": "#55BBAF","username": "Ahmed Soliman"},
     "txt": "Hello World",
     "time": 1398593810519
     }
  }
}
```

The fields of the object `Message` is subject to change in the future, now it only contains the `txt` body of the message but it will include more information in the future. The proposed `Message` format in the future will look like this one

```
{ "#": 55, 
  "time": Timestamp, 
  "sender": {"color": "#55BBAF","username": "Ahmed Soliman"}, 
  "txt": "text-contents of body", 
  "mentions": [/*mention*/], 
  "attachments":[/*attachment*/]
}
```
## Persistent Events
When you send a message to the room, it gets persisted in the Cassandra store, this is not true only for messages but also for the following Events

  - Identification Request
  - Setting Topic

Those events take a sequence number same as messages so they can be traversed and replayed by the server

#### Proposed Ideas [Under Development/Disabled By Default]

  //HEART BEAT
  {"heart-beat": 1} //1 can be any number
  {"heart-beat-ack": 1} //returns the same number


  //Server then will synchronize the member list and replay the last 50 messages to you
  SERVER: {"r": "8BjK8", "o": "room-welcome", "data": {
      "members" : [/*identity*/],
  }

  OR

  SERVER: {"r": "8BjK8", "o": "join-failed", "reason": {/* error */}}


  //SYNC MESSAGES
  CLIENT: {"r": "8BjK8", "o": "sync", "data": {
    "from": seqNr
  }

  SERVER: {"r": "8BjK8", "o": "sync", "data" {
    "messages": []
  }


 //Leave Room
 CLIENT: {"r": "8c662", "o": "leave"}
 SERVER: {"r": "8c66s", "o": "room-bye"}

  // Events
  CLIENT: {"r": "8BjK8", "o": "bt"} //server will distribute as is (begin typing) (o: Operation) (r: Room)
  SERVER to distribute: {"r": "8BjK8", "o": "bt", "data": {"sender" : Username, "when": Timestamp}}
  CLIENT: {"r": "8BjK8", "o": "st"} //server will distribute as is (stop typing)
  CLIENT: {"r": "8BjK8", "o": "away"} //Client can should send if the user left the room browser tab
  
  //Sending Message
  {"r": "8BjK8", "o": "msg", "data": {"message": /* message */}} // (msg does not contain a "nr" field and no "time" (it's generated by server))
  // Receiving Message
  {"r": "8BjK8", "o": "msg", "data": {"sender": "username", "message": /* message+SeqNr */}, "when": Timestamp} // (msg does not contain a "nr" field and no "time" (it's generated by server))

  //Get previous messages; client should send the ID of last message he received
  CLIENT: {"r": "8BjK8", "o": "load", "data": {"last-msg-nr": 23}}
  //Server will respond with a fixed number(10) of messages before this message ID,
  //Server will reply with empty list of messages if it reached the begining of history
  SERVER: {"r": "8BjK8", "o": "load", "data": {"messages": [{/*Messages*/}]}}
  
  //Authentication/Identification
  //Client -> Server
  {identity: "Ahmed Soliman", mail: "ahmed@farghal.com", [token="accessToken"]}
  
  //Server -> Client
  {identified: true} // or error (name exists, similar errors)
  
  
  // Data Structures
  
  // Message
  {"#": 55, "time": "ISO-DateTime in EJSON format", "sid": "66Gb771399", "txt": "text-contents of body", "mentions": [/*mention*/], "attachments":[/*attachment*/]} // (nr: Number, sid: Sender ID)
  //Client Message
  {"sid": "66Gb771399", "body": "text-contents of body", "mentions": [/*mention*/], "attachments":[/*attachment*/]} // (nr: Number, sid: Sender ID)
  
  //Events
  //Begin Typing
  {"r": "8BjK8", "sid": "66Gb771399", "o": "bt", "time": "ISO-DateTime in EJSON format"}
  //Stopped Typing
  {"r": "8BjK8", "sid": "66Gb771399", "o": "st", "time": "ISO-DateTime in EJSON format"}
  //Away
  {"r": "8BjK8", "sid": "66Gb771399", "o": "away", "time": "ISO-DateTime in EJSON format"}
  //Disconnected, Server will distribute this even if the client channel got closed
  {"r": "8BjK8", "sid": "66Gb771399", "o": "disconnect", "time": "ISO-DateTime in EJSON format"}
  
  // Identified User
  {"username": "AhmedSoliman", "color": "#22BBFF", verified: false}