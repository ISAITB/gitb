class MessageController

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'bodyText']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @bodyText) ->

    @$scope.close = () =>
      @$modalInstance.dismiss()

@controllers.controller 'MessageController', MessageController
