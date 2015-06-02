class ErrorController
  name: 'ErrorController'

  @$inject = ['$scope', '$log', '$modalInstance', 'error']

  constructor: (@$scope, @$log, @$modalInstance, error) ->
    @$log.debug "Constructing #{@name}"

    @$scope.error = error

    @$scope.close = () =>
      @$modalInstance.close()

@ControllerUtils.register @controllers, ErrorController
