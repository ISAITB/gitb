class HtmlController

  @$inject = ['$log', '$uibModalInstance', '$scope', 'headerText', 'html']
  constructor: (@$log, @$uibModalInstance, @$scope, @headerText, @html) ->

    @$scope.close = () =>
      @$uibModalInstance.dismiss()

@controllers.controller 'HtmlController', HtmlController
