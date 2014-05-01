'use strict'

window.Instalk or= {}

Instalk.Protocol =
  welcome: (msg) ->
    console.log(msg)
    if msg.welcome == 1
      return msg.user
    else
      throw new Instalk.Errors.ProtocolError("NotWelcome", "We are expecting a welcome from server, instead we got #{msg}")

  handleMessage: (data, callbacks) ->
    console.log callbacks
    switch data.o
      when "room-welcome"
        callback data for callback in callbacks.roomJoined
      when "joined"
        callback data for callback in callbacks.joined
      when "left"
        console.log("PROTOCOL: Got LEFT")
        callback data for callback in callbacks.left
      else
        console.log("Something I could not handle:")
        console.log data


  joinRoom: (roomId) ->
    r: roomId,
    o: "join"

  initMessage:
    v: '0.1'
