class PopupController
  name: 'PopupController'

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'data']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @data) ->

    @$scope.close = () =>
      @$modalInstance.dismiss()

@ControllerUtils.register @controllers, PopupController