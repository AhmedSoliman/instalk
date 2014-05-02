'use strict'

window.Instalk ?= {}

Instalk.myApp = angular.module('webApp', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute',
  'angular-websocket',
  'angularMoment'
])
  .config ['WebSocketProvider', (WebSocketProvider) ->
    WebSocketProvider
    .prefix('')
    .uri(Instalk.Config.Transport.url)
  ]
  .config ['$routeProvider', ($routeProvider) ->
    $routeProvider
      .when '/:roomId',
        templateUrl: 'views/main.html'
        controller: 'MainCtrl'
      .otherwise
        redirectTo: '/' + Instalk.Utils.mkId(6)
  ]
#  .run (amMoment) ->
#    amMoment.changeLanguage('de')

