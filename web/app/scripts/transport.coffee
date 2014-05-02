'use strict'

Instalk.myApp
  .factory 'InstalkProtocol', ($log, WebSocket) ->
    protocol = Instalk.Protocol
    messages = {}
    connected = false
    initialised = false
    inRoom = false
    user = null
    callbacks =
      welcome: []
      joined: []
      left: []
      roomJoined: []
      newTopic: []
      beginTyping: []
      stopTyping: []
      away: []
      message: []


    WebSocket.onopen () ->
      connected = true
      $log.debug "Sending " + JSON.stringify protocol.initMessage
      WebSocket.send JSON.stringify protocol.initMessage

    WebSocket.onclose () ->
      connected = false
      initialised = false

    WebSocket.onmessage (ev) ->
      data = JSON.parse ev.data
      $log.debug "SERVER:"
      $log.debug ev
      if initialised isnt true
        user = protocol.welcome(data)
        initialised = true
        $log.debug "I'm: #{user}"
        callback user for callback in callbacks.welcome
      else
        event = protocol.handleMessage($log, data, callbacks)

    WebSocket.onerror (e) ->
      $log.error("Cannot Connect to WebSocket: #{e}")
      connected = false
      initialised = false

    WebSocket.onclose (e) ->
      $log.error("WebSocket: CLOSED")
      connected = false
      initialised = false

    # Return
    currentState: () -> WebSocket.states[WebSocket.readyState()]
    isOnline: () -> @currentState() == 'OPEN' and initialised and inRoom
    isInitialised: () -> initialised

    messages: messages

    joinRoom: (roomId) ->
      if initialised
        WebSocket.send JSON.stringify protocol.joinRoom roomId
      else
        throw new Instalk.Errors.IllegalStateError('Not Initialised', 'You cannot joing a room unless your connection is initialised!')

    sendMessage: (roomId, message) ->
      if initialised
        WebSocket.send JSON.stringify protocol.sendMessage($log, roomId, message)
      else
        throw new Instalk.Errors.IllegalStateError('Not Initialised', 'You cannot send a message unless your connection is initialised!')

    onRoomJoin: (callback) ->
      inRoom = true
      callbacks.roomJoined.push callback

    onJoin: (callback) ->
      callbacks.joined.push callback

    onLeft: (callback) ->
      callbacks.left.push callback

    onWelcome: (callback) ->
      callbacks.welcome.push callback

    onMessage: (callback) ->
      callbacks.message.push callback


