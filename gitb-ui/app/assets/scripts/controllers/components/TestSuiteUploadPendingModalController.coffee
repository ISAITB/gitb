class TestSuiteUploadPendingModalController

  @$inject = ['$scope', '$log', '$modalInstance', 'specificationId', 'pendingFolderId', 'ConformanceService']

  constructor: (@$scope, @$log, @$modalInstance, specificationId, pendingFolderId, @ConformanceService) ->
    @$log.debug "Constructing TestSuiteUploadPendingModalController"

    @$scope.specificationId = specificationId
    @$scope.pendingFolderId = pendingFolderId
    @$scope.actionPending = false
    @$scope.actionKeepPending = false
    @$scope.actionDropPending = false
    @$scope.actionCancelPending = false

    @$scope.keepHistory = () =>
      @$scope.actionKeepPending = true
      @$scope.performAction('keep')

    @$scope.dropHistory = () =>
      @$scope.actionDropPending = true
      @$scope.performAction('drop')

    @$scope.cancel = () =>
      @$scope.actionCancelPending = true
      @$modalInstance.dismiss()

    @$scope.performAction = (action) =>
        @$scope.actionPending = true
        @ConformanceService.resolvePendingTestSuite(@$scope.specificationId, @$scope.pendingFolderId, action)
            .then (data) =>
                @$modalInstance.close(data)

@controllers.controller 'TestSuiteUploadPendingModalController', TestSuiteUploadPendingModalController