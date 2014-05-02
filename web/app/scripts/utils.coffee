'use strict'

window.Instalk ?= {}

Instalk.Utils =
  mkId: (length) ->
    possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    (possible.charAt(Math.floor(Math.random() * possible.length)) for i in [1..length]).join('')
