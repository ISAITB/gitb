class SystemTestsController
  @$inject = ['$log', '$scope', '$stateParams', '$state', '$modal', 'ReportService', 'Constants', 'TestSuiteService', 'ConformanceService', 'ErrorService']
  constructor: (@$log, @$scope, @$stateParams, @$state, @$modal, @ReportService, @Constants, @TestSuiteService, @ConformanceService, @ErrorService)->
    @$log.debug 'Constructing SystemTestsController...'

    @systemId = @$stateParams["id"]
    @export = false

    @testResults = []

    @tableColumns = [
      {
        field: 'testCase'
        title: 'Test case'
        sortable: true
      }
      {
        field: 'actorName'
        title: 'Actor'
        sortable: true
      }
      {
        field: 'startTime'
        title: 'Start Time'
        sortable: true
        order: 'desc'
      }
      {
        field: 'endTime'
        title: 'End Time'
        sortable: true
      }
      {
        field: 'result'
        title: 'Result'
        sortable: true
      }
    ]

    @currentPage = 1
    @testResultsCount = null
    @limit = @Constants.TABLE_PAGE_SIZE

    @showFiltering = false

    @startTime = {}
    @startTimeOptions =
      locale:
        format: "DD-MM-YYYY"
      eventHandlers:
        'apply.daterangepicker': @applyTimeFiltering
        'cancel.daterangepicker': @clearStartTimeFiltering

    @endTime = {}
    @endTimeOptions =
      locale:
        format: "DD-MM-YYYY"
      eventHandlers:
        'apply.daterangepicker': @applyTimeFiltering
        'cancel.daterangepicker': @clearEndTimeFiltering

    @sortColumn = 'startTime'
    @sortOrder = 'desc'

    @hasNextPage = null
    @hasPreviousPage = null

    @translation =
      selectAll       : ""
      selectNone      : ""
      reset           : ""
      search          : "Search..."
      nothingSelected : "All"

    @filtering =
      domain :
        all : []
        filter : []
        selection : []
      specification :
        all : []
        filter : []
        selection : []
      testSuite :
        all : []
        filter : []
        selection : []
      testCase :
        all : []
        filter : []
        selection : []
      result :
        all : []
        filter : []
        selection : []

    @getAllDomains()
    @getAllSpecifications()
    @getAllTestSuites()
    @getAllTestCases()
    @getAllTestResults()
    @getTestResults()
    @getTestResultsCount()

  getAllDomains: () ->
    @ConformanceService.getDomains()
    .then (data) =>
      @filtering.domain.all = data
      @filtering.domain.filter = JSON.parse(JSON.stringify(data))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getAllSpecifications: () ->
    @ConformanceService.getSpecificationsWithIds()
    .then (data) =>
      @filtering.specification.all = data
      @filtering.specification.filter = JSON.parse(JSON.stringify(data))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getAllTestCases: () ->
    @ReportService.getTestCases()
    .then (data) =>
      @filtering.testCase.all = data
      @filtering.testCase.filter = JSON.parse(JSON.stringify(data))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getAllTestSuites: () ->
    @TestSuiteService.getTestSuitesWithTestCases()
    .then (data) =>
      @filtering.testSuite.all = data
      @filtering.testSuite.filter = JSON.parse(JSON.stringify(data))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getAllTestResults: () ->
    for k, v of @Constants.TEST_CASE_RESULT
      @filtering.result.all.push { result: v }
      @filtering.result.filter.push { result: v }

  getTestResults:() ->
    specIds = _.map @filtering.specification.selection, (s) -> s.id
    testSuiteIds = _.map @filtering.testSuite.selection, (s) -> s.id
    testCaseIds = _.map @filtering.testCase.selection, (s) -> s.id
    domainIds = _.map @filtering.domain.selection, (s) -> s.id
    results = _.map @filtering.result.selection, (s) -> s.result
    startTimeBegin = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    startTimeEnd = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeBegin = @endTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeEnd = @endTime.endDate?.format('DD-MM-YYYY HH:mm:ss')

    @ReportService.getTestResults(@systemId, @currentPage, @limit, specIds, testSuiteIds, testCaseIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, @sortColumn, @sortOrder)
    .then (testResultReports) =>
      resultReportsCollection = _ testResultReports
      resultReportsCollection = resultReportsCollection
                    .map (report) ->
                      transformedObject =
                        testCase: if report.test? then report.test.sname else '-'
                        actorName: if report.actor? then report.actor.name else '-'
                        startTime: report.result.startTime
                        endTime: if report.result.endTime? then report.result.endTime else '-'
                        result: report.result.result
                        sessionId: report.result.sessionId
                      transformedObject
      @testResults = resultReportsCollection.value()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getTestResultsCount: () ->
    specIds = _.map @filtering.specification.selection, (s) -> s.id
    testSuiteIds = _.map @filtering.testSuite.selection, (s) -> s.id
    testCaseIds = _.map @filtering.testCase.selection, (s) -> s.id
    domainIds = _.map @filtering.domain.selection, (s) -> s.id
    results = _.map @filtering.result.selection, (s) -> s.result
    startTimeBegin = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    startTimeEnd = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeBegin = @endTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeEnd = @endTime.endDate?.format('DD-MM-YYYY HH:mm:ss')

    @ReportService.getTestResultsCount(@systemId, specIds, testSuiteIds, testCaseIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd)
    .then (data) =>
      @testResultsCount = data.count
    .then () =>
      @setPaginationStatus()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  clearFiltering: () =>
    @showFiltering = false

    @filtering.domain.filter = JSON.parse(JSON.stringify(@filtering.domain.all))
    @filtering.specification.filter = JSON.parse(JSON.stringify(@filtering.specification.all))
    @filtering.testSuite.filter = JSON.parse(JSON.stringify(@filtering.testSuite.all))
    @filtering.testCase.filter = JSON.parse(JSON.stringify(@filtering.testCase.all))
    @filtering.result.filter = JSON.parse(JSON.stringify(@filtering.result.all))

    @filtering.domain.selection = []
    @filtering.specification.selection = []
    @filtering.testSuite.selection = []
    @filtering.testCase.selection = []
    @filtering.result.selection = []

    @startTime = {}
    @endTime = {}

    @queryDatabase()

  applyFiltering: () =>
    @showFiltering = true

  applyTimeFiltering: (ev, picker) =>
    @queryDatabase()

  clearStartTimeFiltering: (ev, picker) =>
    @clearTimeFiltering @startTime

  clearEndTimeFiltering: (ev, picker) =>
    @clearTimeFiltering @endTime

  clearTimeFiltering: (time) =>
    time.endDate = null
    time.startDate = null
    @queryDatabase()

  sortTestResults: (column) =>
    @sortColumn = column.field
    @sortOrder = column.order
    @getTestResults()

  navigateFirstPage: () =>
    @currentPage = 1
    @queryDatabase()

  navigatePreviousPage: () =>
    @currentPage = @currentPage - 1
    @queryDatabase()

  navigateNextPage: () =>
    @currentPage = @currentPage + 1
    @queryDatabase()

  navigateLastPage: () =>
    @currentPage = Math.ceil(@testResultsCount / @limit)
    @queryDatabase()

  queryDatabase: () ->
    @getTestResults()
    @getTestResultsCount()

  setPaginationStatus: () ->
    if @currentPage == 1
      @hasNextPage = @testResultsCount > @limit
      @hasPreviousPage = false
    else if @currentPage == Math.ceil(@testResultsCount / @limit)
      @hasNextPage = false
      @hasPreviousPage = true
    else
      @hasNextPage = true
      @hasPreviousPage = true

  domainTicked: (domain) =>
    @updateFilteringOptions 'domain', 'specification', (s, v) -> `s.id == v.domain`
    @updateFilteringOptions 'specification', 'testSuite', (s, v) -> `s.id == v.specification`
    @updateFilteringOptions 'testSuite', 'testCase', (s, v) -> v.id in (_.map s.testCases, (testCase) -> testCase.id)
    @queryDatabase()

  specificationTicked: (spec) =>
    @updateFilteringOptions 'specification', 'testSuite', (s, v) -> `s.id == v.specification`
    @updateFilteringOptions 'testSuite', 'testCase', (s, v) -> v.id in (_.map s.testCases, (testCase) -> testCase.id)
    @queryDatabase()

  testSuiteTicked: (testSuite) =>
    @updateFilteringOptions 'testSuite', 'testCase', (s, v) -> v.id in (_.map s.testCases, (testCase) -> testCase.id)
    @queryDatabase()

  testCaseTicked: (testCase) =>
    @queryDatabase()

  organizationTicked: (organization) =>
    @updateFilteringOptions 'organization', 'system', (s, v) -> `s.id == v.owner`
    @queryDatabase()

  resultClicked: (result) =>
    @queryDatabase()

  updateFilteringOptions: (changedFilter, filterToUpdate, matches) ->
    result = []
    if @filtering[changedFilter].selection.length > 0
      for s, i in @filtering[changedFilter].selection
        for v, i in @filtering[filterToUpdate].all
          if matches(s, v)
            found = _.find @filtering[filterToUpdate].filter, (f) =>
              `f.id == v.id`
            v.ticked = if found?.ticked? then found.ticked else false
            result.push v
    else
      result = @filtering[filterToUpdate].all
    @filtering[filterToUpdate].filter = result

  onTestSelect: (test, element) =>
    if @export
        @export = false
    else if @check
        @check = false
    else
        @$state.go 'app.reports.presentation', {session_id: test.sessionId}

  onReportExport: (data) =>
    @export = true
    @ReportService.exportTestCaseReport(data.sessionId, data.testCase)
    .then (stepResults) =>
        a = window.document.createElement('a')
        a.href = window.URL.createObjectURL(new Blob([stepResults], {type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'}));
        a.download = 'report.docx'

        document.body.appendChild(a)
        a.click();

        document.body.removeChild(a)

  onCheckboxCheck: (data) =>
    @check = true

    @showExportButton = false

    for result in @testResults
        if result.checked
            @showExportButton = true

  exportSelected: () =>
    session_ids = []
    testcase_ids = []

    for result in @testResults
        if result.checked
            session_ids.push(result.sessionId)
            testcase_ids.push(result.testCase)

    @ReportService.exportTestCaseReports(session_ids.join(), testcase_ids.join())
     .then (stepResults) =>
        a = window.document.createElement('a')
        a.href = window.URL.createObjectURL(new Blob([stepResults], {type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'}));
        a.download = 'report.docx'

        document.body.appendChild(a)
        a.click();

        document.body.removeChild(a)

@controllers.controller 'SystemTestsController', SystemTestsController
