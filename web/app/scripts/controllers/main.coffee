'use strict'

Array::toDict = (key) ->
  @reduce ((dict, obj) -> dict[ obj[key] ] = obj if obj[key]?; return dict), {}

unless Array::filter
  Array::filter = (callback) ->
    element for element in this when callback(element)

Instalk.myApp
  .controller 'MainCtrl', ['$scope', '$log', '$routeParams',  '$cookies', 'InstalkProtocol', ($scope, $log, $routeParams, $cookies, InstalkProtocol) ->
    _inRoom = false
    $scope.roomId = $routeParams.roomId
    $scope.room =
      topic: ""
    $scope.user = null
    $scope.members = {}
    $scope.messages = []

    InstalkProtocol.onRoomWelcome (data) ->
      #actual init...
      $log.debug "Room #{$scope.roomId} Joined, Members:", data.data.members
      _inRoom = true
      $log.debug "SYNC:", data.data
      $scope.members = data.data.members.toDict 'username'
      $scope.messages = data.data.messages
      $scope.room.topic = data.data.topic

    InstalkProtocol.onWelcome (user) ->
      $log.debug 'Got Welcome...'
      $scope.user = user
      $cookies.userInfo = JSON.stringify user
      InstalkProtocol.joinRoom $scope.roomId

    InstalkProtocol.onJoin (data) ->
      $log.debug "#{data.data.user.username} joined the room"
      $scope.members[data.data.user.username] = data.data.user
      $scope.messages.push data

    InstalkProtocol.onLeft (data) ->
      delete $scope.members[data.data.user.username]
      $log.debug "User: #{data.data.user.username} Left Room"
      $scope.messages.push data

    InstalkProtocol.onMessage (data) ->
      $log.debug 'Adding Message To History:', data
      $scope.messages.push data

    InstalkProtocol.onRoomTopicChange (data) ->
      $scope.messages.push data
      $scope.room.topic = data.data.topic

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


    $scope.away = () -> alert 'We are away!' #TODO

    $scope.isSomeoneTyping = () -> false #TODO

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
      $log.debug 'Sending: ', $scope.msg
      InstalkProtocol.sendMessage $scope.roomId, $scope.msg
      $scope.msg = ''
    ]
