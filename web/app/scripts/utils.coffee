'use strict'

window.Utils or= {}

window.Utils =
  mkId: (length) ->
    possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    (possible.charAt(Math.floor(Math.random() * possible.length)) for i in [1..length]).join('')