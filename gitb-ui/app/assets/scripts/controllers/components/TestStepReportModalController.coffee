class TestStepReportModalController

  @$inject = ['$scope', '$log', '$uibModalInstance', 'step', 'report', 'ReportService', 'sessionId', 'ErrorService']

  constructor: (@$scope, @$log, @$uibModalInstance, step, report, @ReportService, sessionId, @ErrorService) ->
    @$log.debug "Constructing TestStepReportModalController"

    @$scope.step = step
    @$scope.report = report
    @$scope.sessionId = sessionId
    @$scope.exportDisabled = false

    @$scope.export = () =>
        @$scope.exportDisabled = true
        pathForReport = step.report.path
        if !pathForReport?
          pathForReport = step.id + '.xml'
        @ReportService.exportTestStepReport(@$scope.sessionId, escape(pathForReport))
            .then (data) =>
                @$scope.exportDisabled = false
                blobData = new Blob([data], {type: 'application/pdf'});
                saveAs(blobData, "report.pdf");
            .catch (error) =>
                @ErrorService.showErrorMessage(error)

    @$scope.close = () =>
      @$uibModalInstance.dismiss()

@controllers.controller 'TestStepReportModalController', TestStepReportModalController
