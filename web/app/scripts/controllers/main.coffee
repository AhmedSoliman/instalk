'use strict'

Array::toDict = (key) ->
  @reduce ((dict, obj) -> dict[ obj[key] ] = obj if obj[key]?; return dict), {}


Instalk.myApp
  .controller 'MainCtrl', ($scope, $log, $routeParams, InstalkProtocol) ->
    $log.info "HELLO WORLD"
    $scope.members = ['Ahmed', 'Mohamed', 'Ali']
    $scope.messages = ['Hello World', 'Hello Big World']
    $scope.addRandomMember = () -> $scope.members.push("ZAKI" + Math.round(Math.random() * 100))
    $scope.roomId = $routeParams.roomId
    $scope.socket = InstalkProtocol
    $scope.user = null
    $scope.members = {}
    InstalkProtocol.onRoomJoin (data) ->
      #actual init...
      $log.debug "WE JOINED THE ROOM #{$scope.roomId}, MEMBERS: #{data.data.members}"
      $log.debug data.data.members
      $scope.members = data.data.members.toDict("username")

    InstalkProtocol.onWelcome (user) ->
      $log.debug("Server said WELCOME")
      $scope.user = user
      InstalkProtocol.joinRoom $scope.roomId

    InstalkProtocol.onJoin (data) ->
      $log.debug "New Guys Joined #{data.data.user.username}"
      $scope.members[data.data.user.username] = data.data.user

    InstalkProtocol.onLeft (data) ->
      $log.debug "SOMEONE LEFT"
      delete $scope.members[data.data.user.username]
      $log.debug "User: #{data.data.user.username} Left Room"

    $scope.away = () -> alert "We are away!"
