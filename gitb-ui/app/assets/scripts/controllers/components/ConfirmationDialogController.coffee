class ConfirmationDialogController

  @$inject = ['$log', '$uibModalInstance', '$scope', 'headerText', 'bodyText', 'actionButtonText', 'closeButtonText', 'sameStyles', 'oneButton']
  constructor: (@$log, @$uibModalInstance, @$scope, @headerText, @bodyText, @actionButtonText, @closeButtonText, @sameStyles, @oneButton) ->

    @$scope.ok = () =>
      @$uibModalInstance.close()

    @$scope.cancel = () =>
      @$uibModalInstance.dismiss()

    @$scope.okClass = () =>
      "btn btn-default"

    @$scope.cancelClass = () =>
      if (@sameStyles?)
        @$scope.okClass()
      else
        "btn btn-default"

@controllers.controller 'ConfirmationDialogController', ConfirmationDialogController
