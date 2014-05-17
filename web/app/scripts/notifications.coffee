'use strict'

Instalk.myApp
  .factory 'notificationApiService', ['$log', '$timeout', '$rootScope', ($log, $timeout, $rootScope) ->
    $log.info("Notification Service Started")
  ]
