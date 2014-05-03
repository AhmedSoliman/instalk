'use strict'

Array::toDict = (key) ->
  @reduce ((dict, obj) -> dict[ obj[key] ] = obj if obj[key]?; return dict), {}


Instalk.myApp
  .controller 'MainCtrl', ['$scope', '$log', '$routeParams', 'InstalkProtocol', ($scope, $log, $routeParams, InstalkProtocol) ->
    _inRoom = false
    $scope.roomId = $routeParams.roomId
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

    InstalkProtocol.onWelcome (user) ->
      $log.debug 'Got Welcome...'
      $scope.user = user
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

    $scope.sendMessage = () ->
      $log.debug 'Sending: ', $scope.msg
      InstalkProtocol.sendMessage $scope.roomId, $scope.msg
      $scope.msg = ''
    ]
