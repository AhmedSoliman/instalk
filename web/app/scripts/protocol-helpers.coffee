'use strict'

window.Instalk ?= {}

Instalk.Protocol =
  getUser: (msg) ->
    console.log(msg)
    if msg.welcome == 1
      return msg.user
    else
      throw new Instalk.Errors.ProtocolError('NotWelcome', 'We are expecting a welcome from server, instead we got ' + JSON.stringify(msg))

  handleMessage: ($log, data, callbacks) ->
    switch data.o
      when 'room-welcome'
        $log.debug 'Protocol :: ROOM-WELCOME:', data
        callback data for callback in callbacks['roomWelcome']
      when 'joined'
        $log.debug 'Protocol :: JOIN:', data
        callback data for callback in callbacks['joined']
      when 'left'
        $log.debug 'Protocol :: LEFT:' , data
        callback data for callback in callbacks['left']
      when 'msg'
        $log.debug 'Protocol :: MSG:', data
        callback data for callback in callbacks['message']
      when 'set-user-info'
        $log.debug 'Protocol :: USER-INFO:', data
        callback data for callback in callbacks['userInfoUpdate']
      when 'set-room-topic'
        $log.debug 'Protocol :: SET-ROOM-TOPIC:', data
        callback data for callback in callbacks['setRoomTopic']
      when 'bt'
        $log.debug 'Protocol :: BeginTyping :', data
        callback data for callback in callbacks['beginTyping']
      when 'st'
        $log.debug 'Protocol :: StopTyping :', data
        callback data for callback in callbacks['stopTyping']
      else
        $log.error 'Protocol :: ERROR :: I could not handle:', data

  updateUserInfo: ($log, name, color) ->
    r: '*',
    o: 'set-user-info',
    data:
      name: name
      color: color

  sendMessage: ($log, roomId, message) ->
    r: roomId,
    o: 'msg',
    data:
      txt: message

  beginTyping: (room) ->
    r: room,
    o: "bt"

  stopTyping: (room) ->
    r: room,
    o: "st"

  setRoomTopic: (roomId, topic) ->
    r: roomId,
    o: 'set-room-topic',
    data:
      topic: topic

  joinRoom: (roomId) ->
    r: roomId,
    o: 'join'

  initMessage:
    v: '0.1'
