class PopupController

  @$inject = ['$log', '$uibModalInstance', '$scope', 'headerText', 'data']
  constructor: (@$log, @$uibModalInstance, @$scope, @headerText, @data) ->

    @$scope.close = () =>
      @$uibModalInstance.dismiss()

@controllers.controller 'PopupController', PopupController
