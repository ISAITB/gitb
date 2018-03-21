class ConfirmationDialogController

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'bodyText', 'actionButtonText', 'closeButtonText']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @bodyText, @actionButtonText, @closeButtonText) ->

    @$scope.ok = () =>
      @$modalInstance.close()

    @$scope.cancel = () =>
      @$modalInstance.dismiss()

@controllers.controller 'ConfirmationDialogController', ConfirmationDialogController
