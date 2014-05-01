'use strict'

Instalk.myApp
  .factory 'InstalkProtocol', ($log, WebSocket) ->
    protocol = Instalk.Protocol
    connected = false
    initialised = false
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
        $log.error "Handling:"
        $log.error data
        $log.error "WHILE:"
        $log.error ev.data
        event = protocol.handleMessage(data, callbacks)

    WebSocket.onerror (e) ->
      $log.error("Cannot Connect to WebSocket: #{e}")
      connected = false
      initialised = false

    WebSocket.onclose (e) ->
      $log.error("WebSocket: CLOSED")
      connected = false
      initialised = false

    # Return
    isConnected: () -> connected
    isInitialised: () -> initialised

    joinRoom: (roomId) ->
      if initialised
        WebSocket.send JSON.stringify protocol.joinRoom roomId
      else
        throw new Instalk.Errors.IllegalStateError('Not Initialised', 'You cannot joing a room unless your connection is initialised!')

    onRoomJoin: (callback) ->
      callbacks.roomJoined.push(callback)

    onJoin: (callback) ->
      callbacks.joined.push(callback)

    onLeft: (callback) ->
      callbacks.left.push(callback)

    onWelcome: (callback) ->
      callbacks.welcome.push(callback)


