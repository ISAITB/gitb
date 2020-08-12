class TestSuiteUploadPendingModalController

  @$inject = ['$scope', '$q', '$uibModalInstance', 'specificationId', 'pendingFolderId', 'ConformanceService', 'ConfirmationDialogService', 'ErrorService', 'DataService']

  constructor: (@$scope, @$q, @$uibModalInstance, specificationId, pendingFolderId, @ConformanceService, @ConfirmationDialogService, @ErrorService, @DataService) ->

    @$scope.DataService = @DataService
    @$scope.specificationId = specificationId
    @$scope.pendingFolderId = pendingFolderId
    @$scope.actionPending = false
    @$scope.actionProceedPending = false
    @$scope.actionCancelPending = false
    @$scope.choiceMetadataReadonly = false

    @$scope.choiceHistory = 'keep'
    @$scope.choiceMetadata = 'skip'

    @$scope.historyChoiceChanged = () =>
      if @$scope.choiceHistory == 'keep'
        @$scope.choiceMetadataReadonly = false
      else
        @$scope.choiceMetadataReadonly = true
        @$scope.choiceMetadata = 'update'

    @$scope.cancel = () =>
      @$scope.actionCancelPending = true
      @$uibModalInstance.dismiss()

    @$scope.proceed = () =>
      deferred = @$q.defer()
      if @$scope.choiceHistory == 'drop'
        @ConfirmationDialogService.confirm("Confirm test history deletion", "Are you sure you want to delete all existing test sessions for this test suite?", "Yes", "No")
        .then(
          () =>
            deferred.resolve()
          , 
          () =>
            deferred.reject()
        )
      else
        deferred.resolve()
      @$q.all()
      deferred.promise.then(
        () =>
          @$scope.actionPending = true
          @$scope.actionProceedPending = true
          @ConformanceService.resolvePendingTestSuite(@$scope.specificationId, @$scope.pendingFolderId, 'proceed', @$scope.choiceHistory, @$scope.choiceMetadata)
              .then (data) =>
                @$uibModalInstance.close(data)
              .catch (error) =>
                @ErrorService.showErrorMessage(error)
                @$scope.actionPending = false
                @$scope.actionProceedPending = false
      )

@controllers.controller 'TestSuiteUploadPendingModalController', TestSuiteUploadPendingModalController
