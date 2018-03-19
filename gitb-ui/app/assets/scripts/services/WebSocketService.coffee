# Provides a wrapper service for WebSockets
class WebSocketService

	@$inject = ['$log', 'Constants']
	constructor: (@$log, @Constants) ->
		@$log.debug "Constructing WebSocketService"

	createWebSocket:(parameters) ->
		ws = new WebSocket jsRoutes.controllers.WebSocketService.socket("session").webSocketURL()
		delete ws.URL
		ws.onopen    = parameters.callbacks.onopen
		ws.onclose   = parameters.callbacks.onclose
		ws.onerror   = parameters.callbacks.onerror
		ws.onmessage = parameters.callbacks.onmessage
		ws

services.service('WebSocketService', WebSocketService)