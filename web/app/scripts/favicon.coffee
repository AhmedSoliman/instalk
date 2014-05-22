'use strict'

Instalk.myApp.factory 'faviconService', [ '$log', ($log) ->
  favico = new Favico({
    animation : 'fade',
    bgColor: '#00DF59'

  })

  badge = (num) ->
    favico.badge(num)

  reset = () ->
    favico.reset()

  return {
    badge : badge,
    reset : reset
  }
]
