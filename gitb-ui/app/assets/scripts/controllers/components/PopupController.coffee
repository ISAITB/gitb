class PopupController

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'data']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @data) ->

    @$scope.close = () =>
      @$modalInstance.dismiss()

@controllers.controller 'PopupController', PopupController
