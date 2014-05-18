'use strict'

Instalk.myApp.factory 'faviconService', [ '$log', ($log) ->
  favico = new Favico({
    position: 'up'

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
