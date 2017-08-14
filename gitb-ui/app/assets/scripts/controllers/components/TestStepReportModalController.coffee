class TestStepReportModalController

  @$inject = ['$scope', '$log', '$modalInstance', 'step', 'report', 'ReportService']

  constructor: (@$scope, @$log, @$modalInstance, step, report, @ReportService) ->
    @$log.debug "Constructing TestStepReportModalController"

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

@controllers.controller 'TestStepReportModalController', TestStepReportModalController
