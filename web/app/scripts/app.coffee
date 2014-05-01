'use strict'

window.Instalk or= {}

Instalk.myApp = angular.module('webApp', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute',
  'angular-websocket'
])
  .config (WebSocketProvider) ->
    WebSocketProvider
    .prefix('')
    .uri(Config.Transport.url)
  .config ($routeProvider) ->
    $routeProvider
      .when '/:roomId',
        templateUrl: 'views/main.html'
        controller: 'MainCtrl'
      .otherwise
        redirectTo: '/' + Utils.mkId(6)
  # .factory 'InstalkProtocol', ($log, WebSocket) ->
  #     connected = false
  #     $log.info("INSTALK FACTORY")
  #     WebSocket.onopen () ->
  #       connected = true
  #     WebSocket.onclose () ->
  #       connected = false
  #     isConnected: () -> connected
