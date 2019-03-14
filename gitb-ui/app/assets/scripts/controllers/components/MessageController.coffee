class MessageController

  @$inject = ['$log', '$uibModalInstance', '$scope', 'headerText', 'bodyText']
  constructor: (@$log, @$uibModalInstance, @$scope, @headerText, @bodyText) ->

    @$scope.close = () =>
      @$uibModalInstance.dismiss()

@controllers.controller 'MessageController', MessageController
