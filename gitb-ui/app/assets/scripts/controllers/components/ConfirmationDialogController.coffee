class ConfirmationDialogController

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'bodyText', 'actionButtonText', 'closeButtonText', 'sameStyles', 'oneButton']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @bodyText, @actionButtonText, @closeButtonText, @sameStyles, @oneButton) ->

    @$scope.ok = () =>
      @$modalInstance.close()

    @$scope.cancel = () =>
      @$modalInstance.dismiss()

    @$scope.okClass = () =>
      "btn btn-default"

    @$scope.cancelClass = () =>
      if (@sameStyles?)
        @$scope.okClass()
      else
        "btn btn-default"

@controllers.controller 'ConfirmationDialogController', ConfirmationDialogController
