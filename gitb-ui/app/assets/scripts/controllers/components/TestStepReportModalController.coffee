class TestStepReportModalController

  @$inject = ['$scope', '$log', '$modalInstance', 'step', 'report', 'ReportService']

  constructor: (@$scope, @$log, @$modalInstance, step, report, @ReportService) ->
    @$log.debug "Constructing TestStepReportModalController"

    @$scope.step = step
    @$scope.report = report
    @$scope.exportDisabled = false

    @$scope.export = () =>
        @$scope.exportDisabled = true
        pathForReport = step.report.path
        if !pathForReport?
          pathForReport = step.report.tcInstanceId + '/' + step.id + '.xml'
        @ReportService.exportTestStepReport(pathForReport)
            .then (data) =>
                # blobData = new Blob([data], {type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'});
                @$scope.exportDisabled = false
                blobData = new Blob([data], {type: 'application/pdf'});
                saveAs(blobData, "report.pdf");

    @$scope.close = () =>
      @$modalInstance.dismiss()

@controllers.controller 'TestStepReportModalController', TestStepReportModalController
