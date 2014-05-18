'use strict'

Array::toDict = (key) ->
  @reduce ((dict, obj) -> dict[ obj[key] ] = obj if obj[key]?; return dict), {}

unless Array::filter
  Array::filter = (callback) ->
    element for element in this when callback(element)

Instalk.myApp
  .controller 'MainCtrl', ['$scope', 'visibilityApiService', 'faviconService', '$rootScope', '$timeout', '$log', '$routeParams',  '$cookies', 'InstalkProtocol', ($scope, visibilityApiService, faviconService, $rootScope, $timeout, $log, $routeParams, $cookies, InstalkProtocol) ->
    $log.debug("Starting up controller...")
    if InstalkProtocol.isInitialised()
      InstalkProtocol.reconnect true

    _inRoom = false
    _retrier = null
    _hidden = false
    _autoScrollEnabled = true
    _autoScrollSuspended = false
    $scope.scrolledToBottom = ($event, isEnded) ->
      if not _autoScrollSuspended
        if isEnded
          _autoScrollEnabled = true
          stopMarkingMessages()
        else
          startMarkingMessages()
          _autoScrollEnabled = false

    _retryBase = 1
    _unread = 0
    _resetTitle = $rootScope.title
    _titleAnimation = false
    marker =
      o: 'marker'
    _markerLoc = -1
    $scope.roomId = $routeParams.roomId
    $scope.room =
      topic: ""
    $scope.user = null
    $scope.form = {}
    $scope.members = {}
    $scope.messages = []
    $scope.chatEvents =
      areWeTyping: false
      whoIsTyping: []
    $scope.retryAfter = 0

    scrollToBottom = () ->
      if _autoScrollEnabled
        _autoScrollSuspended = true
        $('#messages').animate({
                        scrollTop: $('#messages').last()[0].scrollHeight
                    }, 150, () ->
                      $log.info "Animation Completed"
                     _autoScrollSuspended = false
                     )

    enableAnimateTitle = (resetTitle) ->
      _resetTitle = resetTitle
      faviconService.badge(_unread)
      if not _titleAnimation
        _titleAnimation = true
        $rootScope.title = _resetTitle
        animator = () ->
          if _titleAnimation
            $rootScope.title = $rootScope.title.substring(3)
            if $rootScope.title.length is 0
              $rootScope.title = _resetTitle
            $timeout(animator, 1000)
        $timeout(animator, 1000)

    disableAnimateTitle = () ->
       _titleAnimation = false

    InstalkProtocol.onRoomWelcome (data) ->
      #actual init...
      $log.debug "Room #{$scope.roomId} Joined, Members:", data.data.members
      _inRoom = true
      $log.debug "SYNC:", data.data
      $scope.members = data.data.members.toDict 'username'
      #debugger
      $scope.messages = data.data.messages
      $scope.room.topic = data.data.topic
      $timeout(scrollToBottom, 500)

    isMarked = () -> _markerLoc > -1

    addMarker = () ->
      if not isMarked()
        $log.info "Adding Marker"
        _markerLoc = ($scope.messages.push marker) - 1

    removeMarkers = () ->
      if isMarked()
        $log.info "Removing Marker", _markerLoc
        $log.info "Messages before:", $scope.messages
        $log.info("Removing:", $scope.messages.splice(_markerLoc, 1))
        $log.info "Messages now:", $scope.messages
        _markerLoc = -1

    setTitle = (title) ->
      $rootScope.title = 'Instalk | #' + $scope.roomId + ' ' + title

    formatTitle = (title) ->
      'Instalk | #' + $scope.roomId + ' ' + title

    InstalkProtocol.onWelcome (user) ->
      if _retrier then $timeout.cancel(_retrier)
      _retryBase = 1
      $log.debug 'Got Welcome...'
      $scope.user = user
      $cookies.userInfo = JSON.stringify user
      InstalkProtocol.joinRoom $scope.roomId

    InstalkProtocol.onJoin (data) ->
      $log.debug "#{data.data.user.username} joined the room"
      $scope.members[data.data.user.username] = data.data.user
      $scope.messages.push data
      scrollToBottom()


    InstalkProtocol.onLeft (data) ->
      delete $scope.members[data.data.user.username]
      $log.debug "User: #{data.data.user.username} Left Room"
      $scope.messages.push data
      scrollToBottom()


    InstalkProtocol.onMessage (data) ->
      $log.debug 'Adding Message To History:', data
      if _hidden
        _unread += 1
        addMarker()
        enableAnimateTitle(formatTitle("(#{_unread})"))
      $scope.messages.push data
      scrollToBottom()

    InstalkProtocol.onRoomTopicChange (data) ->
      $scope.messages.push data
      $scope.room.topic = data.data.topic
      scrollToBottom()

    InstalkProtocol.onBeginTyping (data) ->
      if data.data.sender isnt $scope.user.username
        $log.debug("Someone started typing:", data.data.sender)
        if data.data.sender not in $scope.chatEvents.whoIsTyping
          $log.debug(data.data.sender + " IS typing...")
          $scope.chatEvents.whoIsTyping.push data.data.sender

    InstalkProtocol.onStopTyping (data) ->
      if data.data.sender isnt $scope.user.username
        $log.debug("Someone stopped typing:", data.data.sender)
        i = $scope.chatEvents.whoIsTyping.indexOf(data.data.sender)
        $scope.chatEvents.whoIsTyping.splice(i, 1)

    InstalkProtocol.onUserInfoUpdate (data) ->
      $scope.messages.push data
      #check if it's me or not first
      if $scope.user.username is data.data.originalUsername
        $log.debug 'Updating my own data to ', data.data.newUserInfo
        $scope.user = data.data.newUserInfo
        $cookies.userInfo = JSON.stringify $scope.user
      else
        #search in members
        $log.debug 'Updating a member data to ', data.data.newUserInfo
        delete $scope.members[data.data.originalUsername]
        $scope.members[data.data.newUserInfo.username] = data.data.newUserInfo
      scrollToBottom()

    handleConnectionDrop = () ->
      if _retrier then $timeout.cancel(_retrier)
      $log.debug("We lost connection")
      _retryBase += 1
      if _retryBase > 7
        _retryBase = 7
      $scope.retryAfter = Math.pow(2, _retryBase)
      _retrier = $timeout(retryDecay, 1000)

    InstalkProtocol.onConnectionDrop handleConnectionDrop

    retryDecay = () ->
      if _retrier then $timeout.cancel(_retrier)
      $scope.retryAfter -= 1
      if $scope.retryAfter <= 0
        #it's time to retry
        $scope.reconnect()
      else
        _retrier = $timeout(retryDecay, 1000)

    scheduleStopTyping = () ->
      $scope.chatEvents.timer = $timeout(stopTyping, 2000)

    $scope.isSomeoneTyping = () -> $scope.chatEvents.whoIsTyping.length > 0

    $scope.whoIsTyping = () ->
      names = $scope.chatEvents.whoIsTyping.map (w) ->
        $scope.members[w]?.info.name
      names.join(', ')


    $scope.beginTyping = (ev) ->
      keycode = ev.which
      if (keycode >= 0) and (keycode > 19) and (keycode isnt 224) and (keycode isnt 91) and (keycode not in [13, 37, 38, 39, 40])
        if $scope.chatEvents.areWeTyping and $scope.chatEvents.timer
          $timeout.cancel($scope.chatEvents.timer)
          scheduleStopTyping()
        else
          $log.debug("We started typing...:", ev)
          InstalkProtocol.beginTyping $scope.roomId
          $scope.chatEvents.areWeTyping = true
          scheduleStopTyping()

    stopTyping = () ->
      $log.debug("We stopped Typing")
      if $scope.chatEvents.timer
        $log.debug("Cancelling timer...")
        $timeout.cancel($scope.chatEvents.timer)
      else
        $log.debug("No timer to cancel")
      $scope.chatEvents.areWeTyping = false
      InstalkProtocol.stopTyping $scope.roomId

    $scope.getLag = () -> InstalkProtocol.getLag()
    $scope.isConnecting = () ->
      (InstalkProtocol.currentState() is 'OPEN' or InstalkProtocol.currentState() is 'CONNECTING') and not $scope.isOnline()

    $scope.isDisconnected = () ->
      (InstalkProtocol.currentState() is 'CLOSED') or (InstalkProtocol.currentState() is 'CLOSING')

    $scope.isConnected = () -> InstalkProtocol.currentState() == 'OPEN'

    $scope.isOnline = () -> InstalkProtocol.isOnline() and _inRoom is true

    $scope.reconnect = () ->
      if _retrier then $timeout.cancel(_retrier)
      InstalkProtocol.reconnect()

    $scope.currentState = () -> InstalkProtocol.currentState()

    $scope.initialisationStatus = () ->
      switch InstalkProtocol.currentState()
        when 'OPEN'
          if InstalkProtocol.isInitialised()
            if _inRoom then 'Ready...' else 'Joining Room...'
          else 'Initialising...'
        when 'CONNECTING' then 'Connecting...'
        else 'Unknown...'

    $scope.updateUserInfo =  () ->
      InstalkProtocol.updateUserInfo $scope.user.info.name, $scope.user.info.color

    $scope.setRoomTopic = () ->
      $log.info("Updating the room topic to:", $scope.room.topic)
      InstalkProtocol.setRoomTopic $scope.roomId, $scope.room.topic

    $scope.sendMessage = () ->
      stopTyping()
      $log.debug 'Sending: ', $scope.form.msg
      _autoScrollEnabled = true
      InstalkProtocol.sendMessage $scope.roomId, $scope.form.msg
      $scope.form.msg = ''

    $scope.$on '$destroy', () ->
      $log.debug("Controller is dying...")

    if $scope.isDisconnected()
      _retryBase = 1
      if _retrier then $timeout.cancel(_retrier)
      handleConnectionDrop()

    startMarkingMessages = () ->
      _hidden = true
      _autoScrollEnabled = false

    stopMarkingMessages = () ->
      _hidden = false
      _unread = 0
      setTitle('')
      disableAnimateTitle()
      faviconService.reset()

    $scope.$on 'visibilityChanged', (event, isHidden) ->
      $log.info("Visibility Changed", event, isHidden)
      if isHidden
        removeMarkers()
        startMarkingMessages()
      else
        stopMarkingMessages()
      $scope.$apply()
      $rootScope.$apply()

    ]
