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

    @$scope.copyToClipboard = () =>
      cm = $('.CodeMirror')[0].CodeMirror
      cm.focus()
      cm.execCommand "selectAll"
      try
        success = document.execCommand 'copy'
      catch err
        # Show message to user? ignore?

class TestStepReportModalController
  name: 'TestStepReportModalController'

  @$inject = ['$scope', '$log', '$modalInstance', 'step', 'report', 'ReportService']

  constructor: (@$scope, @$log, @$modalInstance, step, report, @ReportService) ->
    @$log.debug "Constructing #{@name}"

    @$scope.step = step
    @$scope.report = report

    @$scope.export = () =>
        @ReportService.exportTestStepReport(step.report.path)
            .then (data) =>
                a = window.document.createElement('a')
                a.href = window.URL.createObjectURL(new Blob([data], {type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'}));
                a.download = report.id + ".docx"

                document.body.appendChild(a)
                a.click();
                document.body.removeChild(a)

    @$scope.close = () =>
      @$modalInstance.dismiss()

@ControllerUtils.register @controllers, TestStepReportModalController
@ControllerUtils.register @controllers, EditorModalController
