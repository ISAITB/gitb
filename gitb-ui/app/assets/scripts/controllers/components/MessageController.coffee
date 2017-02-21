class MessageController
  name: 'MessageController'

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'bodyText']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @bodyText) ->

    @$scope.close = () =>
      @$modalInstance.dismiss()

@ControllerUtils.register @controllers, MessageController