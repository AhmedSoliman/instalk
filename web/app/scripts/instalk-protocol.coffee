'use strict'

window.Instalk ?= {}

Instalk.Protocol =
  welcome: (msg) ->
    console.log(msg)
    if msg.welcome == 1
      return msg.user
    else
      throw new Instalk.Errors.ProtocolError("NotWelcome", "We are expecting a welcome from server, instead we got #{msg}")

  handleMessage: ($log, data, callbacks) ->
    switch data.o
      when "room-welcome"
        callback data for callback in callbacks.roomJoined
      when "joined"
        $log.debug("PROTOCOL: Got JOIN: #{data}")
        callback data for callback in callbacks.joined
      when "left"
        $log.debug("PROTOCOL: Got LEFT: #{data}")
        callback data for callback in callbacks.left
      when "msg"
        $log.debug("PROTOCOL: Got MSG: #{data}")
        callback data for callback in callbacks.message

      else
        $log.error("Something I could not handle:" + data)

  sendMessage: ($log, roomId, message) ->
    r: roomId,
    o: "msg",
    data:
      txt: message


  joinRoom: (roomId) ->
    r: roomId,
    o: "join"

  initMessage:
    v: '0.1'
