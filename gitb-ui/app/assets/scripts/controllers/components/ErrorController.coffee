class ErrorController

  @$inject = ['$scope', '$log', '$uibModalInstance', '$sce', 'Constants', 'error', 'withRetry']

  constructor: (@$scope, @$log, @$uibModalInstance, @$sce, @Constants, error, withRetry) ->
    @$log.debug "Constructing #{@name}"

    @$scope.title = 'Unexpected Error'
    if error.statusText?
      @$scope.title = error.statusText

    @$scope.error = error
    @$scope.withRetry = withRetry

    errorMessage = '-'
    if error.data?.error_description?
      errorMessage = error.data.error_description
    errorContent = error.template.split(@Constants.PLACEHOLDER__ERROR_DESCRIPTION).join(errorMessage)

    if error.data?.error_id?
      errorContent = errorContent.split(@Constants.PLACEHOLDER__ERROR_ID).join(error.data.error_id)
    @$scope.error.messageToShow = @$sce.trustAsHtml(errorContent)

    @$scope.retry = () =>
      @$uibModalInstance.close()

    @$scope.close = () =>
      @$uibModalInstance.dismiss()

@controllers.controller 'ErrorController', ErrorController

