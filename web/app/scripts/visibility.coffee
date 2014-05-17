'use strict'

Instalk.myApp
  .factory 'visibilityApiService', ['$log', '$timeout', '$rootScope', ($log, $timeout, $rootScope) ->
    visibilitychanged = () ->
      $rootScope.$broadcast 'visibilityChanged', document.hidden or document.webkitHidden or document.mozHidden or document.msHidden

    document.addEventListener "visibilitychange", visibilitychanged
    document.addEventListener "webkitvisibilitychange", visibilitychanged
    document.addEventListener "msvisibilitychange", visibilitychanged

  ]

