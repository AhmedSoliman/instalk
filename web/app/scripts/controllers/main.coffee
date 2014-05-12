'use strict'

Array::toDict = (key) ->
  @reduce ((dict, obj) -> dict[ obj[key] ] = obj if obj[key]?; return dict), {}

unless Array::filter
  Array::filter = (callback) ->
    element for element in this when callback(element)

Instalk.myApp
  .controller 'MainCtrl', ['$scope', '$timeout', '$log', '$routeParams',  '$cookies', 'InstalkProtocol', ($scope, $timeout, $log, $routeParams, $cookies, InstalkProtocol) ->
    $log.debug("Starting up controller...")
    if InstalkProtocol.isInitialised()
      InstalkProtocol.reconnect true

    _inRoom = false
    $scope.roomId = $routeParams.roomId
    $scope.room =
      topic: ""
    $scope.user = null
    $scope.form = {}
    $scope.members = {}
    $scope.messages = []
    $scope.chatEvents =
      areWeTyping: false
      whoIsTyping: []


    scrollToBottom = () ->
      $('#messages').animate({
                        scrollTop: $('#messages').last()[0].scrollHeight
                    }, 500)

    InstalkProtocol.onRoomWelcome (data) ->
      #actual init...
      $log.debug "Room #{$scope.roomId} Joined, Members:", data.data.members
      _inRoom = true
      $log.debug "SYNC:", data.data
      $scope.members = data.data.members.toDict 'username'
      $scope.messages = data.data.messages
      $scope.room.topic = data.data.topic
      $timeout(scrollToBottom, 500)


    InstalkProtocol.onWelcome (user) ->
      $log.debug 'Got Welcome...'
      $scope.user = user
      $cookies.userInfo = JSON.stringify user
      InstalkProtocol.joinRoom $scope.roomId

    InstalkProtocol.onJoin (data) ->
      $log.debug "#{data.data.user.username} joined the room"
      $scope.members[data.data.user.username] = data.data.user
      $scope.messages.push data
      scrollToBottom()


    InstalkProtocol.onLeft (data) ->
      delete $scope.members[data.data.user.username]
      $log.debug "User: #{data.data.user.username} Left Room"
      $scope.messages.push data
      scrollToBottom()


    InstalkProtocol.onMessage (data) ->
      $log.debug 'Adding Message To History:', data
      $scope.messages.push data
      scrollToBottom()

    InstalkProtocol.onRoomTopicChange (data) ->
      $scope.messages.push data
      $scope.room.topic = data.data.topic
      scrollToBottom()

    InstalkProtocol.onBeginTyping (data) ->
      if data.data.sender isnt $scope.user.username
        $log.info("Someone started typing:", data.data.sender)
        if data.data.sender not in $scope.chatEvents.whoIsTyping
          $log.info(data.data.sender + " IS typing...")
          $scope.chatEvents.whoIsTyping.push data.data.sender

    InstalkProtocol.onStopTyping (data) ->
      if data.data.sender isnt $scope.user.username
        $log.info("Someone stopped typing:", data.data.sender)
        i = $scope.chatEvents.whoIsTyping.indexOf(data.data.sender)
        $scope.chatEvents.whoIsTyping.splice(i, 1)

    InstalkProtocol.onUserInfoUpdate (data) ->
      $scope.messages.push data
      #check if it's me or not first
      if $scope.user.username is data.data.originalUsername
        $log.debug 'Updating my own data to ', data.data.newUserInfo
        $scope.user = data.data.newUserInfo
        $cookies.userInfo = JSON.stringify $scope.user
      else
        #search in members
        $log.debug 'Updating a member data to ', data.data.newUserInfo
        delete $scope.members[data.data.originalUsername]
        $scope.members[data.data.newUserInfo.username] = data.data.newUserInfo
      scrollToBottom()


    scheduleStopTyping = () ->
      $scope.chatEvents.timer = $timeout(stopTyping, 2000)

    $scope.isSomeoneTyping = () -> $scope.chatEvents.whoIsTyping.length > 0

    $scope.whoIsTyping = () ->
      names = $scope.chatEvents.whoIsTyping.map (w) ->
        $scope.members[w]?.info.name
      names.join(', ')


    $scope.beginTyping = (ev) ->
      keycode = ev.which
      if (keycode >= 0) and (keycode > 19) and (keycode isnt 224) and (keycode isnt 91) and (keycode not in [13, 37, 38, 39, 40])
        if $scope.chatEvents.areWeTyping is true and $scope.chatEvents.timer
          $timeout.cancel($scope.chatEvents.timer)
          scheduleStopTyping()
        else
          $log.debug("We started typing...:", ev)
          InstalkProtocol.beginTyping $scope.roomId
          $scope.chatEvents.areWeTyping = true
          scheduleStopTyping()

    stopTyping = () ->
      $log.debug("We stopped Typing")
      InstalkProtocol.stopTyping $scope.roomId
      if $scope.chatEvents.timer
        $timeout.cancel($scope.chatEvents.timer)
      $timeout( () ->
        $scope.chatEvents.areWeTyping = false
      , 300)

    $scope.isConnecting = () ->
      (InstalkProtocol.currentState() is 'OPEN' or InstalkProtocol.currentState() is 'CONNECTING') and not $scope.isOnline()

    $scope.isDisconnected = () ->
      (InstalkProtocol.currentState() is 'CLOSED') or (InstalkProtocol.currentState() is 'CLOSING')

    $scope.isConnected = () -> InstalkProtocol.currentState() == 'OPEN'

    $scope.isOnline = () -> InstalkProtocol.isOnline() and _inRoom is true

    $scope.reconnect = () -> InstalkProtocol.reconnect()

    $scope.currentState = () -> InstalkProtocol.currentState()

    $scope.initialisationStatus = () ->
      switch InstalkProtocol.currentState()
        when 'OPEN'
          if InstalkProtocol.isInitialised()
            if _inRoom then 'Ready...' else 'Joining Room...'
          else 'Initialising...'
        when 'CONNECTING' then 'Connecting...'
        else 'Unknown...'

    $scope.updateUserInfo =  () ->
      InstalkProtocol.updateUserInfo $scope.user.info.name, $scope.user.info.color

    $scope.setRoomTopic = () ->
      $log.info("Updating the room topic to:", $scope.room.topic)
      InstalkProtocol.setRoomTopic $scope.roomId, $scope.room.topic

    $scope.sendMessage = () ->
      if $scope.chatEvents.timer and $scope.chatEvents.areWeTyping
        $log.debug("Canceling the timer and stopping immediately")
        $timeout.cancel($scope.chatEvents.timer)
        stopTyping()
      $log.debug 'Sending: ', $scope.form.msg
      InstalkProtocol.sendMessage $scope.roomId, $scope.form.msg
      $scope.form.msg = ''

    $scope.$on '$destroy', () ->
      $log.debug("Controller is dying...")


    ]
