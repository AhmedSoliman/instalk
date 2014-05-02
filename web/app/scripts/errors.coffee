'use strict'

window.Instalk ?= {}

window.Instalk.Errors ?= {}

class InstalkException
  constructor: (@name, @message) ->
  toString: () -> "#{@name}: #{@message}"


Instalk.Errors =
  InstalkError: InstalkException

  IllegalStateError:
    class IllegalStateError extends InstalkException

  ProtocolError:
    class ProtocolError extends InstalkException

  SocketError:
    class SocketError extends InstalkException
