'use strict';

angular.module('instalk', [
  'ngCookies',
  'ngResource',
  'ngSanitize',
  'ngRoute'
])
  .config(function ($routeProvider) {
      $routeProvider
      .when('/:roomId', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl'
      })
      .otherwise({
        redirectTo: '/' + Utils.mkId()
      });
  });
