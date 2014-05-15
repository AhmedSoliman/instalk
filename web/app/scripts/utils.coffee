'use strict'

window.Instalk ?= {}

hidden = "hidden"

if (hidden of document)
  document.addEventListener("visibilitychange", onchange)
else if ((hidden is "mozHidden") of document)
  document.addEventListener("mozvisibilitychange", onchange)
else if ((hidden is "webkitHidden") of document)
  document.addEventListener("webkitvisibilitychange", onchange)
else if ((hidden is "msHidden") of document)
  document.addEventListener("msvisibilitychange", onchange)
# IE 9 and lower:
else if ('onfocusin' of document)
  document.onfocusin = document.onfocusout = onchange
# All others:
else
  window.onpageshow = window.onpagehide = window.onfocus = window.onblur = onchange


Instalk.Utils =
  onVisibilityChange: (evt) ->
    v = 'visible'
    h = 'hidden'
    evtMap =
      focus:v,
      focusin: v ,
      pageshow:v,
      blur:h,
      focusout:h,
      pagehide:h
    evt = evt or window.event
    if (evt.type of evtMap)
      document.body.className = evtMap[evt.type]
    else
      document.body.className = if hidden then "hidden" else "visible"

  mkId: (length) ->
    possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    (possible.charAt(Math.floor(Math.random() * possible.length)) for i in [1..length]).join('')

