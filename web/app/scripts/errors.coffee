'use strict'

window.Instalk or= {}

window.Instalk.Errors or= {}

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
