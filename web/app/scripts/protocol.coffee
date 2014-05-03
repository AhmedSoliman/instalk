'use strict'

Instalk.myApp
  .factory 'InstalkProtocol', ['$log', '$timeout', 'WebSocket', ($log, $timeout, WebSocket) ->
    _initialised = false
    _callbacks = {}

    onBeforeWelcomeMessage = (ev) ->
      data = JSON.parse ev.data
      user = Instalk.Protocol.getUser data
      _initialised = true
      $log.debug 'I am:', user
      WebSocket.onmessage onAfterWelcomeMessage
      callback user for callback in _callbacks['welcome']

    onAfterWelcomeMessage = (ev) ->
      data = JSON.parse ev.data
      Instalk.Protocol.handleMessage $log, data, _callbacks

    reconnect = () ->
      WebSocket.onmessage onBeforeWelcomeMessage
      $log.info 'Reconnecting to the WebSocket...'
      WebSocket.new()

    WebSocket.onopen () ->
      $log.info 'WebSocket Connected...'
      $log.debug 'Initialising...'
      WebSocket.send JSON.stringify Instalk.Protocol.initMessage

    WebSocket.onclose () ->
      $log.info 'WebSocket Closed...'
      _initialised = false

    WebSocket.onmessage onBeforeWelcomeMessage

    WebSocket.onerror (e) ->
      $log.error 'Error: Lost Connection to WebSocket:', e
      _initialised = false

    registerEvent = (topic, callback) ->
      (_callbacks[topic] ?=[]).push callback


    # Return
    reconnect: () ->  reconnect()
    currentState: () -> WebSocket.states[WebSocket.readyState()]
    isOnline: () -> @currentState() == 'OPEN' and _initialised
    isInitialised: () -> _initialised

    joinRoom: (roomId) ->
      if _initialised
        WebSocket.send JSON.stringify Instalk.Protocol.joinRoom roomId
      else
        throw new Instalk.Errors.IllegalStateError('Not Initialised', 'You cannot joing a room unless your connection is _initialised!')

    sendMessage: (roomId, message) ->
      if _initialised
        WebSocket.send JSON.stringify Instalk.Protocol.sendMessage($log, roomId, message)
      else
        throw new Instalk.Errors.IllegalStateError('Not Initialised', 'You cannot send a message unless your connection is _initialised!')

    onRoomWelcome: (callback) ->
      registerEvent 'roomWelcome', callback

    onJoin: (callback) ->
      registerEvent 'joined', callback

    onLeft: (callback) ->
      registerEvent 'left', callback

    onWelcome: (callback) ->
      registerEvent 'welcome', callback

    onMessage: (callback) ->
      registerEvent 'message', callback
    ]


