# Provides a wrapper service for WebSockets
class ErrorService

  @$inject = ['$log', '$modal', 'Constants']
  constructor: (@$log, @$modal, @Constants) ->
    @$log.debug "Constructing ErrorService"

  showErrorMessage: (error, withRetry) ->
    modalOptions =
      templateUrl: 'assets/views/components/error-modal.html'
      controller: 'ErrorController as errorCtrl'
      resolve:
        error: () => error
        withRetry: () => withRetry

    @$modal.open modalOptions

services.service('ErrorService', ErrorService)
