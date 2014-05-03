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

      else
        $log.error 'Protocol :: ERROR :: I could not handle:', data

  sendMessage: ($log, roomId, message) ->
    r: roomId,
    o: 'msg',
    data:
      txt: message


  joinRoom: (roomId) ->
    r: roomId,
    o: 'join'

  initMessage:
    v: '0.1'
