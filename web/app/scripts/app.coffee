'use strict'

window.Instalk ?= {}

Instalk.myApp = angular.module('webApp', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute',
  'angular-websocket',
  'angularMoment',
  'ngAnimate'
  "xeditable"
])
  .config ['WebSocketProvider', (WebSocketProvider) ->
   userInfo = $.cookie('userInfo')
   uri = Instalk.Config.Transport.url
   if userInfo
      uri += "?user=" + encodeURIComponent(userInfo)
    console.log("THE URI:::" + uri)
    WebSocketProvider
    .prefix('')
    .uri(uri)
  ]
  .config ['$routeProvider', '$locationProvider', ($routeProvider, $locationProvider) ->
    $routeProvider
      .when '/:roomId',
        templateUrl: 'views/main.html'
        controller: 'MainCtrl'
      .otherwise
        redirectTo: '/' + Instalk.Utils.mkId(6)

    $locationProvider.html5Mode(true)
  ]
  .config ['$logProvider', ($logProvider) ->
    $logProvider.debugEnabled(true)
  ]
  .run ['$rootScope', '$log', ($rootScope, $log) ->
    $rootScope.$on '$routeChangeSuccess', (event, currentRoute, previousRoute) ->
      #Change page title, based on Route information
      if currentRoute.title
        $rootScope.title = currentRoute.title
      else if currentRoute.pathParams['roomId']
        $rootScope.title = currentRoute.pathParams['roomId']
      else
        $rootScope.title = "Let's Talk!"
  ]
