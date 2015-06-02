# Provides a wrapper service for WebSockets
class ErrorService

	constructor: (@$log, @$modal, @Constants) ->
		@$log.debug "Constructing ErrorService"

	showErrorMessage: (error) ->
		modalOptions =
      templateUrl: 'assets/views/components/error-modal.html'
      controller: 'ErrorController as errorCtrl'
      resolve:
        error: () => error
      size: 'lg'

    @$modal.open modalOptions

services.service('ErrorService', ErrorService)
