class ErrorController

  @$inject = ['$scope', '$log', '$modalInstance', 'error', 'withRetry']

  constructor: (@$scope, @$log, @$modalInstance, error, withRetry) ->
    @$log.debug "Constructing #{@name}"

    @$scope.error = error
    @$scope.withRetry = withRetry

    @$scope.retry = () =>
      @$modalInstance.close()

    @$scope.close = () =>
      @$modalInstance.dismiss()

@controllers.controller 'ErrorController', ErrorController

