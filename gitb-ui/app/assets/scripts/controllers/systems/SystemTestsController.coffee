class SystemTestsController
  @$inject = ['$log', '$scope', '$stateParams', '$state', '$modal', 'ReportService', 'ErrorService']
  constructor: (@$log, @$scope, @$stateParams, @$state, @$modal, @ReportService, @ErrorService)->
    @$log.debug 'Constructing SystemTestsController...'

    @systemId = @$stateParams["id"]
    @export = false

    @tableColumns = [
      {
        field: 'testName'
        title: 'Test case'
      }
      {
        field: 'actorName'
        title: 'Actor'
      }
      {
        field: 'startTime',
        title: 'Start Time'
      }
      {
        field: 'endTime',
        title: 'End Time'
      }
      {
        field: 'result'
        title: 'Result'
      }
    ]

    @getTestResults()


  getTestResults:() ->
    @ReportService.getTestResults(@systemId, 0, 20)
    .then (testResultReports) =>
      resultReportsCollection = _ testResultReports
      resultReportsCollection = resultReportsCollection
                    .sortBy (report) -> report.result.startTime
                    .map (report) ->
                      transformedObject =
                        testName: if report.test? then report.test.sname else '-'
                        actorName: if report.actor? then report.actor.name else '-'
                        startTime: report.result.startTime
                        endTime: if report.result.endTime? then report.result.endTime else '-'
                        result: report.result.result
                        sessionId: report.result.sessionId
                      transformedObject
                    .reverse()
      @testResults = resultReportsCollection.value()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  onTestSelect: (test, element) =>
    if @export
        @export = false
    else
        @$state.go 'app.reports.presentation', {session_id: test.sessionId}

  onReportExport: (data) =>
    @export = true
    @ReportService.exportTestCaseReport(data.sessionId)
    .then (stepResults) =>
        a = window.document.createElement('a')
        a.href = window.URL.createObjectURL(new Blob([stepResults], {type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'}));
        a.download = 'report.docx'

        document.body.appendChild(a)
        a.click();

        document.body.removeChild(a)

@controllers.controller 'SystemTestsController', SystemTestsController
