class HtmlController
  name: 'HtmlController'

  @$inject = ['$log', '$modalInstance', '$scope', 'headerText', 'html']
  constructor: (@$log, @$modalInstance, @$scope, @headerText, @html) ->

    @$scope.close = () =>
      @$modalInstance.dismiss()

@ControllerUtils.register @controllers, HtmlController