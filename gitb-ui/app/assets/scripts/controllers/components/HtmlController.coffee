class HtmlController

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'html']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @html) ->

    @$scope.close = () =>
      @$modalInstance.dismiss()

@controllers.controller 'HtmlController', HtmlController
