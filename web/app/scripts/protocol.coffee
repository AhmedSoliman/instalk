'use strict'

Instalk.myApp
  .factory 'InstalkProtocol', ['$log', '$timeout', 'WebSocket', ($log, $timeout, WebSocket) ->
    _initialised = false
    _callbacks = {}
    _timeout = null
    _lag = 0

    setHeartbeatTimer = () ->
      i = new Date().getTime()
      die = () ->
        $log.warn("We didn't receive the heart beat for quite sometime, dying...")
        _callbacks['heartbeat'] = []
        _timeout = null
        WebSocket.close()

      _timeout = $timeout(die, 4000) # we will die after 4 seconds from now if nobody set off this timer

      _callbacks['heartbeat'] = [(data) ->
        if _initialised
          ret = new Date().getTime()
          if data['heart-beat-ack'] is i
            $log.info("Heartbeat, lag: #{ret - i}ms")
            _lag = ret - i
            $timeout.cancel(_timeout)
            _timeout = $timeout(setHeartbeatTimer, 14000)
          else
            $log.warn("Got the wrong heart-beat-ack, this is potentially something crazy!")
      ]

      $log.debug("Sending Heartbeat:", i)
      WebSocket.send JSON.stringify Instalk.Protocol.heartbeat(i)

    onBeforeWelcomeMessage = (ev) ->
      data = JSON.parse ev.data
      user = Instalk.Protocol.getUser data
      _initialised = true
      $log.debug 'I am:', user
      WebSocket.onmessage onAfterWelcomeMessage
      if _callbacks['welcome'] then callback user for callback in _callbacks['welcome']
      _lastMessage = new Date().getTime()
      setHeartbeatTimer()

    onAfterWelcomeMessage = (ev) ->
      data = JSON.parse ev.data
      Instalk.Protocol.handleMessage $log, data, _callbacks

    reconnect = (resetHandlers) ->
      $log.debug("RESET HANDLERS:", resetHandlers)
      if _timeout then $timeout.cancel(_timeout)
      if resetHandlers
        $log.debug("Cleaning up WebSocket handlers...")
        _callbacks = {}
        _initialised = false
        try
          WebSocket.onclose () -> undefined
          WebSocket.onmessage () -> undefined
          WebSocket.close()
          WebSocket.onopen () -> undefined
          WebSocket.onerror () -> undefined
        catch error
          $log.warn("could not close the old websocket")
      $log.info 'Reconnecting to the WebSocket...'
      WebSocket.new()
      WebSocket.onopen onOpen
      WebSocket.onmessage onBeforeWelcomeMessage
      WebSocket.onerror onError
      WebSocket.onclose onClose

    onOpen = () ->
      $log.info 'WebSocket Connected...'
      $log.debug 'Initialising...'
      WebSocket.send JSON.stringify Instalk.Protocol.initMessage

    WebSocket.onopen onOpen

    onClose = () ->
      $log.info 'WebSocket Closed...'
      _initialised = false
      if _callbacks['closed'] then callback() for callback in _callbacks['closed']
      if _timeout then $timeout.cancel(_timeout)

    WebSocket.onclose onClose

    WebSocket.onmessage onBeforeWelcomeMessage

    onError = (e) ->
      $log.error 'Error: Lost Connection to WebSocket:', e
      _initialised = false
      # if _callbacks['closed'] then callback() for callback in _callbacks['closed']
      if _timeout then $timeout.cancel(_timeout)

    WebSocket.onerror onError

    registerEvent = (topic, callback) ->
      (_callbacks[topic] ?=[]).push callback


    # Return
    reconnect: (resetHandlers) ->
      reconnect(resetHandlers)


    currentState: () -> WebSocket.states[WebSocket.readyState()]
    isOnline: () -> @currentState() == 'OPEN' and _initialised
    isInitialised: () -> _initialised
    getLag: () -> _lag

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

    updateUserInfo: (name, color) ->
      if _initialised
        WebSocket.send JSON.stringify Instalk.Protocol.updateUserInfo($log, name, color)
      else
        throw new Instalk.Errors.IllegalStateError('Not Initialised', 'You cannot send a message unless your connection is _initialised!')

    setRoomTopic: (room, topic) ->
      if _initialised
        msg = JSON.stringify Instalk.Protocol.setRoomTopic(room, topic)
        $log.info "Setting Room Topic: #{msg}"
        WebSocket.send msg
      else
        throw new Instalk.Errors.IllegalStateError('Not Initialised', 'You cannot send a message unless your connection is _initialised!')

    beginTyping: (room) ->
      WebSocket.send JSON.stringify Instalk.Protocol.beginTyping(room)

    stopTyping: (room) ->
      WebSocket.send JSON.stringify Instalk.Protocol.stopTyping(room)

    onUserInfoUpdate: (callback) ->
      registerEvent 'userInfoUpdate', callback

    onRoomWelcome: (callback) ->
      registerEvent 'roomWelcome', callback

    onJoin: (callback) ->
      registerEvent 'joined', callback

    onLeft: (callback) ->
      registerEvent 'left', callback

    onWelcome: (callback) ->
      $log.debug("Registering for WELCOME")
      registerEvent 'welcome', callback

    onMessage: (callback) ->
      registerEvent 'message', callback

    onRoomTopicChange: (callback) ->
      registerEvent 'setRoomTopic', callback

    onBeginTyping: (callback) ->
      registerEvent 'beginTyping', callback

    onStopTyping: (callback) ->
      registerEvent 'stopTyping', callback

    onConnectionDrop: (callback) ->
      registerEvent 'closed', callback

    ]


