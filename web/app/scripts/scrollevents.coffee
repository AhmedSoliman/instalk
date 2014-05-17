'use strict';
INTERVAL_DELAY = 150;

Instalk.myApp
  .directive 'ngScrollEnd', ['$parse', '$window', '$log', ($parse, $window, $log) ->
    (scope, element, attr) ->
      handler = null
      isEnded = false
      fn = $parse attr.ngScrollEnd
      el = element[0]
      scrollEvent = 'scroll'

      bindScroll = () ->
        handler = (event) ->
          if (el.offsetHeight + el.scrollTop >= el.scrollHeight)
            if not isEnded
              scrollTrigger event, true
              isEnded = true
          else
            if isEnded
              scrollTrigger event, false
              isEnded = false
        element.bind scrollEvent, handler

      # unbindScroll = () ->
      #   # be nice to others, don't unbind their scroll handlers
      #   element.unbind(scrollEvent, handler)

      scrollTrigger = (event, isEnded) ->
        scope.$apply () ->
          fn(scope, {$event: event, isEnded: isEnded})

      bindScroll()
  ]
