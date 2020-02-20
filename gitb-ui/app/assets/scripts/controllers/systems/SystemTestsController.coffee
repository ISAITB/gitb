class SystemTestsController
  @$inject = ['$log', '$q', '$scope', '$stateParams', '$state', '$uibModal', 'CommunityService', '$window', 'ReportService', 'Constants', 'TestSuiteService', 'ConformanceService', 'ErrorService', 'DataService', 'ConfirmationDialogService', 'TestService', 'PopupService']
  constructor: (@$log, @$q, @$scope, @$stateParams, @$state, @$uibModal, @CommunityService, @$window, @ReportService, @Constants, @TestSuiteService, @ConformanceService, @ErrorService, @DataService, @ConfirmationDialogService, @TestService, @PopupService)->
    @$log.debug 'Constructing SystemTestsController...'

    @systemId = @$stateParams["id"]
    @export = false

    @activeTests = []
    @testResults = []

    @activeTestsColumns = [
      {
        field: 'specification',
        title: @DataService.labelSpecification(),
        sortable: true
      }
      {
        field: 'actor',
        title: @DataService.labelActor(),
        sortable: true
      }
      {
        field: 'testCaseName',
        title: 'Test case',
        sortable: true
      }
      {
        field: 'startTime',
        title: 'Start time',
        sortable: true
        order: 'asc'
      }
    ]

    @tableColumns = [
      {
        field: 'specification'
        title: @DataService.labelSpecification()
        sortable: true
      }
      {
        field: 'actor'
        title: @DataService.labelActor()
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
    
    @activeSortColumn = "startTime"
    @activeSortOrder = "asc"
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
      @resetFilters(@applySavedSearchState())
      @queryDatabase()

  applySavedSearchState: () =>
    if @DataService.searchState? && @DataService.searchState.data? && @DataService.searchState.origin == @Constants.SEARCH_STATE_ORIGIN.SYSTEM_TESTS
      @savedSearchState = @DataService.searchState.data
      if @savedSearchState.domainIds?
        @filtering.domain.selection = _.map(_.filter(@filtering.domain.all, (d) => @savedSearchState.domainIds.includes(d.id)), _.clone)
      if @savedSearchState.specIds?
        @filtering.specification.selection = _.map(_.filter(@filtering.specification.all, (d) => @savedSearchState.specIds.includes(d.id)), _.clone)
      if @savedSearchState.testSuiteIds?
        @filtering.testSuite.selection = _.map(_.filter(@filtering.testSuite.all, (d) => @savedSearchState.testSuiteIds.includes(d.id)), _.clone)
      if @savedSearchState.testCaseIds?
        @filtering.testCase.selection = _.map(_.filter(@filtering.testCase.all, (d) => @savedSearchState.testCaseIds.includes(d.id)), _.clone)
      if @savedSearchState.results?
        @filtering.result.selection = _.map(_.filter(@filtering.result.all, (d) => @savedSearchState.results.includes(d.result)), _.clone)
        for f in @filtering.result.filter
          found = _.find @filtering.result.selection, (d) => `d.result == f.result`
          if found?
            f.ticked = true            
      @startTime.startDate = @savedSearchState.startTimeBegin
      @startTime.endDate = @savedSearchState.startTimeEnd
      @endTime.startDate = @savedSearchState.endTimeBegin
      @endTime.endDate = @savedSearchState.endTimeEnd
      @currentPage = @savedSearchState.currentPage
      @limit = @savedSearchState.limit
      @activeSortColumn = @savedSearchState.activeSortColumn
      @activeSortOrder = @savedSearchState.activeSortOrder
      for column in @activeTestsColumns
        if column.field == @activeSortColumn
          column.order = @activeSortOrder
        else
          column.order = undefined
      @sortColumn = @savedSearchState.sortColumn
      @sortOrder = @savedSearchState.sortOrder
      for column in @tableColumns
        if column.field == @sortColumn
          column.order = @sortOrder
        else
          column.order = undefined
      @showFiltering = @savedSearchState.showFiltering
      @DataService.clearSearchState()
      @searchState = undefined
      true
    else
      false

  resetFilters: (keepTick) ->
    @setDomainFilter()
    if keepTick
      @setSpecificationFilter(@filtering.domain.selection, @filtering.domain.filter, keepTick)
      @setTestSuiteFilter(@filtering.specification.selection, @filtering.specification.filter, keepTick)
      @setTestCaseFilter(@filtering.testSuite.selection, @filtering.testSuite.filter, keepTick)
    else
      @setSpecificationFilter(@filtering.domain.filter, [], keepTick)
      @setTestSuiteFilter(@filtering.specification.filter, [], keepTick)
      @setTestCaseFilter(@filtering.testSuite.filter, [], keepTick)

  setDomainFilter: () ->
    if @community.domainId?
      id = @community.domainId
      @filtering.domain.filter = _.map(_.filter(@filtering.domain.all, (d) => `d.id == id`), _.clone)
      if @filtering.domain.filter? && @filtering.domain.filter.length > 0
        @filtering.domain.filter[0].ticked = true
      @filtering.domain.selection = _.map(@filtering.domain.filter, _.clone)
    else
      @filtering.domain.filter = _.map(@filtering.domain.all, _.clone)
      if @filtering.domain.selection.length > 0
        for f in @filtering.domain.filter
          found = _.find @filtering.domain.selection, (d) => `d.id == f.id`
          if found?
            f.ticked = true

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
      else
        found.ticked = true

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
      else
        found.ticked = true

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
      else
        found.ticked = true


  keepTickedProperty: (oldArr, newArr) ->
    if oldArr? and oldArr.length > 0
      for o, i in newArr
        n = _.find oldArr, (s) => `s.id == o.id`
        o.ticked = if n?.ticked? then n.ticked else false

  getAllDomains: () ->
    d = @$q.defer()
    @ConformanceService.getDomainsForSystem(@systemId)
    .then (data) =>
      @filtering.domain.all = data
      d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllSpecifications: () ->
    d = @$q.defer()
    @ConformanceService.getSpecificationsForSystem(@systemId)
    .then (data) =>
       @filtering.specification.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllTestCases: () ->
    d = @$q.defer()
    @ReportService.getTestCasesForSystem(@systemId)
    .then (data) =>
       @filtering.testCase.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllTestSuites: () ->
    d = @$q.defer()
    @TestSuiteService.getTestSuitesWithTestCasesForSystem(@systemId)
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

  getCurrentSearchCriteria:() ->
    searchCriteria = {}
    searchCriteria.domainIds = _.map @filtering.domain.selection, (s) -> s.id
    searchCriteria.specIds = _.map @filtering.specification.selection, (s) -> s.id
    searchCriteria.testSuiteIds = _.map @filtering.testSuite.selection, (s) -> s.id
    searchCriteria.testCaseIds = _.map @filtering.testCase.selection, (s) -> s.id
    searchCriteria.results = _.map @filtering.result.selection, (s) -> s.result
    searchCriteria.startTimeBegin = @startTime.startDate
    searchCriteria.startTimeBeginStr = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    searchCriteria.startTimeEnd = @startTime.endDate
    searchCriteria.startTimeEndStr = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    searchCriteria.endTimeBegin = @endTime.startDate
    searchCriteria.endTimeBeginStr = @endTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    searchCriteria.endTimeEnd = @endTime.endDate
    searchCriteria.endTimeEndStr = @endTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    searchCriteria.systemId = @systemId
    searchCriteria.currentPage = @currentPage
    searchCriteria.limit = @limit
    searchCriteria.activeSortColumn = @activeSortColumn
    searchCriteria.activeSortOrder = @activeSortOrder
    searchCriteria.sortColumn = @sortColumn
    searchCriteria.sortOrder = @sortOrder
    searchCriteria.showFiltering = @showFiltering
    searchCriteria

  getActiveTests: () ->
    params = @getCurrentSearchCriteria()

    @ReportService.getSystemActiveTestResults(@systemId, params.specIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.startTimeBeginStr, params.startTimeEndStr, params.activeSortColumn, params.activeSortOrder)
    .then (testResultReports) =>
      resultReportsCollection = _ testResultReports
      resultReportsCollection = resultReportsCollection
                    .map (report) ->
                      transformedObject =
                        specification: if report.test? then report.specification.sname else '-'
                        actor: if report.actor? then report.actor.name else '-'
                        testCaseName: if report.test? then report.test.sname else '-'
                        startTime: report.result.startTime
                        sessionId: report.result.sessionId

                      transformedObject
      @activeTests = resultReportsCollection.value()
      @refreshActivePending = false
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @refreshActivePending = false

  getTestResults:() ->
    params = @getCurrentSearchCriteria()

    @ReportService.getTestResults(params.systemId, params.currentPage, params.limit, params.specIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr, params.sortColumn, params.sortOrder)
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
      @refreshCompletedPending = false
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @refreshCompletedPending = false

  getTestResultsCount: () ->
    params = @getCurrentSearchCriteria()

    @ReportService.getTestResultsCount(params.systemId, params.specIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr)
    .then (data) =>
      @testResultsCount = data.count
      @refreshCompletedCountPending = false
    .then () =>
      @setPaginationStatus()
      @refreshCompletedCountPending = false
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @refreshCompletedCountPending = false

  clearFiltering: () =>
    @showFiltering = false

    @startTime = {}
    @endTime = {}

    @filtering.domain.selection = []
    @filtering.specification.selection = []
    @filtering.testSuite.selection = []
    @filtering.testCase.selection = []
    @filtering.result.selection = []
    for f in @filtering.result.filter
      f.ticked = false

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

  sortActiveSessions: (column) =>
    @activeSortColumn = column.field
    @activeSortOrder = column.order
    @getActiveTests()

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
    @getActiveTests()
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
        searchState = @getCurrentSearchCriteria()
        @DataService.setSearchState(searchState, @Constants.SEARCH_STATE_ORIGIN.SYSTEM_TESTS)
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

  exportActiveSessionsToCsv: () =>
    params = @getCurrentSearchCriteria()

    @ReportService.getSystemActiveTestResults(@systemId, params.specIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.startTimeBeginStr, params.startTimeEndStr, params.activeSortColumn, params.activeSortOrder)
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
                        sessionId: report.result.sessionId
                      transformedObject
      if resultReportsCollection.value().length > 0
        @DataService.exportAllAsCsv([@DataService.labelDomain(), @DataService.labelSpecification(), @DataService.labelActor(), "Test suite", "Test case", "Start time", "Session"], resultReportsCollection.value())
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  exportToCsv: () =>
    params = @getCurrentSearchCriteria()

    @ReportService.getTestResults(params.systemId, 1, 1000000, params.specIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr, params.sortColumn, params.sortOrder)
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
        @DataService.exportAllAsCsv([@DataService.labelDomain(), @DataService.labelSpecification(), @DataService.labelActor(), "Test suite", "Test case", "Start time", "End time", "Result", "Session", "Obsolete"], resultReportsCollection.value())
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  canDelete: () =>
    !@DataService.isVendorUser

  stopSession: (session) =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you certain you want to terminate this session?", "Yes", "No")
    .then () =>
      @TestService.stop(session.sessionId)
      .then (data) =>
        @$state.go @$state.current, {}, {reload: true}
        @PopupService.success('Test session terminated.')
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  deleteObsolete: () ->
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete all obsolete test results?", "Yes", "No")
    .then () =>
      @deletePending = true
      @ConformanceService.deleteObsoleteTestResultsForSystem(@systemId)
      .then () =>
        @deletePending = false
        @$state.go @$state.current, {}, {reload: true}
        @PopupService.success('Obsolete test results deleted.')
      .catch (error) =>
          @deletePending = false
          @ErrorService.showErrorMessage(error)

  refresh: () =>
    @refreshActivePending = true
    @refreshCompletedPending = true
    @refreshCompletedCountPending = true
    @getActiveTests()
    @getTestResults()
    @getTestResultsCount()

@controllers.controller 'SystemTestsController', SystemTestsController
