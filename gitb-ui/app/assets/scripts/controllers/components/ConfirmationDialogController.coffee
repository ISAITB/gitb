class ConfirmationDialogController

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'bodyText', 'actionButtonText', 'closeButtonText', 'sameStyles']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @bodyText, @actionButtonText, @closeButtonText, @sameStyles) ->

    @$scope.ok = () =>
      @$modalInstance.close()

    @$scope.cancel = () =>
      @$modalInstance.dismiss()

    @$scope.okClass = () =>
      "btn btn-primary"

    @$scope.cancelClass = () =>
      if (@sameStyles?)
        @$scope.okClass()
      else
        "btn btn-default"

@controllers.controller 'ConfirmationDialogController', ConfirmationDialogController
