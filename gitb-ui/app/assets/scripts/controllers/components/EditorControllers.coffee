class EditorModalController
  name: 'EditorModalController'

  @$inject = ['$scope', '$log', '$modalInstance', 'name', 'editorOptions', 'indicators', 'lineNumber']

  constructor: (@$scope, @$log, @$modalInstance, documentName, editorOptions, indicators, lineNumber) ->
    @$log.debug "Constructing #{@name}"

    @$scope.isNameVisible = documentName?
    @$scope.documentName = documentName
    @$scope.editorOptions = editorOptions
    @$scope.indicators = indicators
    @$scope.lineNumber = lineNumber

    @$scope.close = () =>
      @$modalInstance.dismiss()

class TestStepReportModalController
  name: 'TestStepReportModalController'

  @$inject = ['$scope', '$log', '$modalInstance', 'step', 'report']

  constructor: (@$scope, @$log, @$modalInstance, step, report) ->
    @$log.debug "Constructing #{@name}"

    @$scope.step = step
    @$scope.report = report

    @$scope.close = () =>
      @$modalInstance.dismiss()

@ControllerUtils.register @controllers, TestStepReportModalController
@ControllerUtils.register @controllers, EditorModalController
