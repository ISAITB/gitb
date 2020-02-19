class TestSuiteUploadPendingModalController

  @$inject = ['$scope', '$log', '$uibModalInstance', 'specificationId', 'pendingFolderId', 'ConformanceService', 'ConfirmationDialogService', 'ErrorService']

  constructor: (@$scope, @$log, @$uibModalInstance, specificationId, pendingFolderId, @ConformanceService, @ConfirmationDialogService, @ErrorService) ->
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
      @ConfirmationDialogService.confirm("Confirm test history deletion", "Are you sure you want to delete all existing test sessions for this test suite?", "Yes", "No")
      .then () =>
        @$scope.actionDropPending = true
        @$scope.performAction('drop')

    @$scope.cancel = () =>
      @$scope.actionCancelPending = true
      @$uibModalInstance.dismiss()

    @$scope.performAction = (action) =>
      @$scope.actionPending = true
      @ConformanceService.resolvePendingTestSuite(@$scope.specificationId, @$scope.pendingFolderId, action)
          .then (data) =>
            @$uibModalInstance.close(data)
          .catch (error) =>
            @ErrorService.showErrorMessage(error)

@controllers.controller 'TestSuiteUploadPendingModalController', TestSuiteUploadPendingModalController
