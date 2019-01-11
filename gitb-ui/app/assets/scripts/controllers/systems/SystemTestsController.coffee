class SystemTestsController
  @$inject = ['$log', '$q', '$scope', '$stateParams', '$state', '$modal', 'CommunityService', '$window', 'ReportService', 'Constants', 'TestSuiteService', 'ConformanceService', 'ErrorService', 'DataService', 'ConfirmationDialogService']
  constructor: (@$log, @$q, @$scope, @$stateParams, @$state, @$modal, @CommunityService, @$window, @ReportService, @Constants, @TestSuiteService, @ConformanceService, @ErrorService, @DataService, @ConfirmationDialogService)->
    @$log.debug 'Constructing SystemTestsController...'

    @systemId = @$stateParams["id"]
    @export = false

    @testResults = []

    @tableColumns = [
      {
        field: 'specification'
        title: 'Specification'
        sortable: true
      }
      {
        field: 'actor'
        title: 'Actor'
        sortable: true
      }
      {
        field: 'testCaseName'
        title: 'Test case'
        sortable: true
      }
      {
        field: 'startTime'
        title: 'Start time'
        sortable: true
        order: 'desc'
      }
      {
        field: 'endTime'
        title: 'End time'
        sortable: true
      }
      {
        field: 'result'
        title: 'Result'
        sortable: true
      }
    ]

    @organization = JSON.parse(@$window.localStorage['organization'])
    @community = JSON.parse(@$window.localStorage['community'])

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

    d1 = @getAllDomains()
    d2 = @getAllSpecifications()
    d3 = @getAllTestSuites()
    d4 = @getAllTestCases()
    d5 = @getAllTestResults()

    @$q.all([d1, d2, d3, d4, d5])
    .then () =>
      @resetFilters(false)
      @queryDatabase()

  resetFilters: (keepTick) ->
    @setDomainFilter()
    @setSpecificationFilter(@filtering.domain.filter, [], keepTick)
    @setTestSuiteFilter(@filtering.specification.filter, [], keepTick)
    @setTestCaseFilter(@filtering.testSuite.filter, [], keepTick)

  setDomainFilter: () ->
    if @community.domainId?
      id = @community.domainId
      @filtering.domain.filter = _.map(_.filter(@filtering.domain.all, (d) => `d.id == id`), _.clone)
      @filtering.domain.filter[0].ticked = true
      @filtering.domain.selection = _.map(@filtering.domain.filter, _.clone)
    else
      @filtering.domain.filter = _.map(@filtering.domain.all, _.clone)

  setSpecificationFilter: (selection1, selection2, keepTick) ->
    selection = if selection1? and selection1.length > 0 then selection1 else selection2
    copy = _.map(@filtering.specification.filter, _.clone)
    @filtering.specification.filter = _.map((_.filter @filtering.specification.all, (s) => (_.contains (_.map selection, (d) => d.id), s.domain)), _.clone)
    @keepTickedProperty(copy, @filtering.specification.filter) if keepTick

    for i in [@filtering.specification.selection.length - 1..0] by -1
      some = @filtering.specification.selection[i]
      found = _.find @filtering.specification.filter, (s) => `s.id == some.id`
      if (!found?)
        @filtering.specification.selection.splice(i, 1)

  setTestSuiteFilter: (selection1, selection2, keepTick) ->
    selection = if selection1? and selection1.length > 0 then selection1 else selection2
    copy = _.map(@filtering.testSuite.filter, _.clone)
    @filtering.testSuite.filter = _.map((_.filter @filtering.testSuite.all, (t) => (_.contains (_.map selection, (s) => s.id), t.specification)), _.clone)
    @keepTickedProperty(copy, @filtering.testSuite.filter) if keepTick

    for i in [@filtering.testSuite.selection.length - 1..0] by -1
      some = @filtering.testSuite.selection[i]
      found = _.find @filtering.testSuite.filter, (s) => `s.id == some.id`
      if (!found?)
        @filtering.testSuite.selection.splice(i, 1)

  setTestCaseFilter: (selection1, selection2, keepTick) ->
    selection = if selection1? and selection1.length > 0 then selection1 else selection2
    copy = _.map(@filtering.testCase.filter, _.clone)
    result = []
    for s, i in selection
      for t, i in s.testCases
        found = _.find @filtering.testCase.all, (c) => `c.id == t.id`
        result.push found
    @filtering.testCase.filter = _.map(result, _.clone)
    @keepTickedProperty(copy, @filtering.testCase.filter) if keepTick

    for i in [@filtering.testCase.selection.length - 1..0] by -1
      some = @filtering.testCase.selection[i]
      found = _.find @filtering.testCase.filter, (s) => `s.id == some.id`
      if (!found?)
        @filtering.testCase.selection.splice(i, 1)

  keepTickedProperty: (oldArr, newArr) ->
    if oldArr? and oldArr.length > 0
      for o, i in newArr
        n = _.find oldArr, (s) => `s.id == o.id`
        o.ticked = if n?.ticked? then n.ticked else false

  getAllDomains: () ->
    d = @$q.defer()
    @ConformanceService.getDomains()
    .then (data) =>
      @filtering.domain.all = data
      d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllSpecifications: () ->
    d = @$q.defer()
    @ConformanceService.getSpecificationsWithIds()
    .then (data) =>
       @filtering.specification.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllTestCases: () ->
    d = @$q.defer()
    @ReportService.getTestCases()
    .then (data) =>
       @filtering.testCase.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllTestSuites: () ->
    d = @$q.defer()
    @TestSuiteService.getTestSuitesWithTestCases()
    .then (data) =>
       @filtering.testSuite.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

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
                        testCaseId: if report.test? then report.test.id else '-'
                        specification: if report.test? then report.specification.sname else '-'
                        testCaseName: if report.test? then report.test.sname else '-'
                        actor: if report.actor? then report.actor.name else '-'
                        startTime: report.result.startTime
                        endTime: if report.result.endTime? then report.result.endTime else '-'
                        result: report.result.result
                        sessionId: report.result.sessionId
                        obsolete: report.result.obsolete
                        hideExportButton: report.result.obsolete
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

    @startTime = {}
    @endTime = {}

    @filtering.domain.selection = []
    @filtering.specification.selection = []
    @filtering.testSuite.selection = []
    @filtering.testCase.selection = []
    @filtering.result.selection = []

    @resetFilters(false)
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
    @setSpecificationFilter(@filtering.domain.selection, @filtering.domain.filter, true)
    @setTestSuiteFilter(@filtering.specification.selection, @filtering.specification.filter, true)
    @setTestCaseFilter(@filtering.testSuite.selection, @filtering.testSuite.filter, true)
    @queryDatabase()

  specificationTicked: (spec) =>
    @setTestSuiteFilter(@filtering.specification.selection, @filtering.specification.filter, true)
    @setTestCaseFilter(@filtering.testSuite.selection, @filtering.testSuite.filter, true)
    @queryDatabase()

  testSuiteTicked: (testSuite) =>
    @setTestCaseFilter(@filtering.testSuite.selection, @filtering.testSuite.filter, true)
    @queryDatabase()

  testCaseTicked: (testCase) =>
    @queryDatabase()

  resultClicked: (result) =>
    @queryDatabase()

  removeFromSelection: (parent, childSelection, matcherFunction) ->
    result = []
    for o, i in childSelection
      if (matcherFunction(parent, o) == false)
        result.push o
    childSelection = result

  onTestSelect: (test, element) =>
    if @export
        @export = false
    else if @check
        @check = false
    else
        @$state.go 'app.reports.presentation', {session_id: test.sessionId}

  rowStyle: (row) => 
    if row.obsolete
      "test-result-obsolete"
    else  
      ""

  onReportExport: (data) =>
    if (!data.obsolete)
      @export = true
      @ReportService.exportTestCaseReport(data.sessionId, data.testCaseId)
      .then (stepResults) =>
          blobData = new Blob([stepResults], {type: 'application/pdf'});
          saveAs(blobData, "report.pdf");
      .catch (error) =>
          @ErrorService.showErrorMessage(error)

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
        blobData = new Blob([stepResults], {type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'});
        saveAs(blobData, "report.docx");

  exportToCsv: () =>
    specIds = _.map @filtering.specification.selection, (s) -> s.id
    testSuiteIds = _.map @filtering.testSuite.selection, (s) -> s.id
    testCaseIds = _.map @filtering.testCase.selection, (s) -> s.id
    domainIds = _.map @filtering.domain.selection, (s) -> s.id
    results = _.map @filtering.result.selection, (s) -> s.result
    startTimeBegin = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    startTimeEnd = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeBegin = @endTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeEnd = @endTime.endDate?.format('DD-MM-YYYY HH:mm:ss')

    @ReportService.getTestResults(@systemId, 1, 1000000, specIds, testSuiteIds, testCaseIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, @sortColumn, @sortOrder)
    .then (testResultReports) =>
      resultReportsCollection = _ testResultReports
      resultReportsCollection = resultReportsCollection
                    .map (report) ->
                      transformedObject =
                        domain: if report.domain? then report.domain.sname else '-'
                        specification: if report.specification? then report.specification.sname else '-'
                        actor: if report.actor? then report.actor.name else '-'
                        testSuite: if report.testSuite? then report.testSuite.sname else '-'
                        testCase: if report.test? then report.test.sname else '-'
                        startTime: report.result.startTime
                        endTime: if report.result.endTime? then report.result.endTime else '-'
                        result: report.result.result
                        sessionId: report.result.sessionId
                        obsolete: report.result.obsolete

                      transformedObject
      if resultReportsCollection.value().length > 0
        csv = ["Domain", "Specification", "Actor", "Test suite", "Test case", "Start time", "End time", "Result", "Session", "Obsolete"].toString() + "\n"
        for o, i in resultReportsCollection.value()
          line = ""
          idx = 0
          for k, v of o
            if idx++ != 0
              line += ","
            if (v?)
              line += String(v).replace /,/, " "
          csv += if i < resultReportsCollection.value().length then line + "\n" else line
        blobData = new Blob([csv], {type: 'text/csv'});
        saveAs(blobData, "export.csv");

  canDelete: () =>
    !@DataService.isVendorUser

  deleteObsolete: () ->
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete all obsolete test results?", "Yes", "No")
    .then () =>
      @deletePending = true
      @ConformanceService.deleteObsoleteTestResultsForSystem(@systemId)
      .then () =>
        @deletePending = false
        @$state.go @$state.current, {}, {reload: true}
      .catch (error) =>
          @deletePending = false
          @ErrorService.showErrorMessage(error)

@controllers.controller 'SystemTestsController', SystemTestsController
