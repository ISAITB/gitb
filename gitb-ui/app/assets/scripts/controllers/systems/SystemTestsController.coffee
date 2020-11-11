class SystemTestsController
  @$inject = ['$log', '$q', '$scope', '$stateParams', '$state', '$uibModal', 'CommunityService', '$window', 'ReportService', 'Constants', 'TestSuiteService', 'ConformanceService', 'ErrorService', 'DataService', 'ConfirmationDialogService', 'TestService', 'PopupService']
  constructor: (@$log, @$q, @$scope, @$stateParams, @$state, @$uibModal, @CommunityService, @$window, @ReportService, @Constants, @TestSuiteService, @ConformanceService, @ErrorService, @DataService, @ConfirmationDialogService, @TestService, @PopupService) ->
    @$log.debug 'Constructing SystemTestsController...'

    @systemId = @$stateParams["id"]
    @export = false
    @exportActivePending = false
    @exportCompletedPending = false
    @activeExpandedCounter = {count: 0}
    @activeDataStatus = {status: @Constants.STATUS.PENDING}
    @completedDataStatus = {status: @Constants.STATUS.PENDING}
    @completedExpandedCounter = {count: 0}

    @organization = JSON.parse(@$window.localStorage['organization'])
    @community = JSON.parse(@$window.localStorage['community'])

    @activeTests = []
    @testResults = []

    @filterState = {
      updatePending: false
    }

    @filters = [@Constants.FILTER_TYPE.SPECIFICATION, @Constants.FILTER_TYPE.ACTOR, @Constants.FILTER_TYPE.TEST_SUITE, @Constants.FILTER_TYPE.TEST_CASE, @Constants.FILTER_TYPE.RESULT, @Constants.FILTER_TYPE.TIME, @Constants.FILTER_TYPE.SESSION]
    if !@community.domain?
      @filters.push(@Constants.FILTER_TYPE.DOMAIN)

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
        sortable: true,
        iconFn: @DataService.iconForTestResult
      }
    ]

    @currentPage = 1
    @testResultsCount = 0
    @limit = @Constants.TABLE_PAGE_SIZE

    @activeSortColumn = "startTime"
    @activeSortOrder = "asc"
    @sortColumn = 'startTime'
    @sortOrder = 'desc'

    @hasNextPage = null
    @hasPreviousPage = null

  getAllDomains: () =>
    @ConformanceService.getDomainsForSystem(@systemId)

  getAllSpecifications: () =>
    @ConformanceService.getSpecificationsForSystem(@systemId)

  getAllActors: () =>
    @ConformanceService.getActorsForSystem(@systemId)

  getAllTestCases: () =>
    @ReportService.getTestCasesForSystem(@systemId)

  getAllTestSuites: () =>
    @TestSuiteService.getTestSuitesWithTestCasesForSystem(@systemId)

  getCurrentSearchCriteria:() =>
    filters = @filterState.currentFilters()
    searchCriteria = {}
    searchCriteria.specIds = filters[@Constants.FILTER_TYPE.SPECIFICATION]
    searchCriteria.actorIds = filters[@Constants.FILTER_TYPE.ACTOR]
    searchCriteria.testSuiteIds = filters[@Constants.FILTER_TYPE.TEST_SUITE]
    searchCriteria.testCaseIds = filters[@Constants.FILTER_TYPE.TEST_CASE]
    if @community.domain?
      searchCriteria.domainIds = [@community.domain.id]
    else
      searchCriteria.domainIds = filters[@Constants.FILTER_TYPE.DOMAIN]
    searchCriteria.results = filters[@Constants.FILTER_TYPE.RESULT]
    searchCriteria.startTimeBeginStr = filters.startTimeBeginStr
    searchCriteria.startTimeEndStr = filters.startTimeEndStr
    searchCriteria.endTimeBeginStr = filters.endTimeBeginStr
    searchCriteria.endTimeEndStr = filters.endTimeEndStr
    searchCriteria.sessionId = filters.sessionId
    searchCriteria.systemId = @systemId
    searchCriteria.currentPage = @currentPage
    searchCriteria.limit = @limit
    searchCriteria.activeSortColumn = @activeSortColumn
    searchCriteria.activeSortOrder = @activeSortOrder
    searchCriteria.sortColumn = @sortColumn
    searchCriteria.sortOrder = @sortOrder
    searchCriteria

  setFilterRefreshState: () =>
    @filterState.updatePending = @refreshActivePending || @refreshCompletedPending

  getActiveTests: () =>
    params = @getCurrentSearchCriteria()
    @refreshActivePending = true
    @setFilterRefreshState()
    @ReportService.getSystemActiveTestResults(@systemId, params.specIds, params.actorIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.startTimeBeginStr, params.startTimeEndStr, params.sessionId, params.activeSortColumn, params.activeSortOrder)
    .then (data) =>
      resultReportsCollection = _ data.data
      resultReportsCollection = resultReportsCollection
                    .map (report) ->
                      transformedObject =
                        specification: if report.test? then report.specification.sname else '-'
                        actor: if report.actor? then report.actor.name else '-'
                        testCaseName: if report.test? then report.test.sname else '-'
                        testSuiteName: if report.testSuite? then report.testSuite.sname else '-'
                        startTime: report.result.startTime
                        sessionId: report.result.sessionId

                      transformedObject
      @activeTests = resultReportsCollection.value()
      @refreshActivePending = false
      @setFilterRefreshState()
      @activeDataStatus.status = @Constants.STATUS.FINISHED
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @refreshActivePending = false
      @setFilterRefreshState()
      @activeDataStatus.status = @Constants.STATUS.FINISHED

  getTestResults:() =>
    params = @getCurrentSearchCriteria()
    @refreshCompletedPending = true
    @setFilterRefreshState()
    @ReportService.getTestResults(params.systemId, params.currentPage, params.limit, params.specIds, params.actorIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr, params.sessionId, params.sortColumn, params.sortOrder)
    .then (data) =>
      @testResultsCount = data.count
      resultReportsCollection = _ data.data
      resultReportsCollection = resultReportsCollection
                    .map (report) ->
                      transformedObject =
                        testCaseId: if report.test? then report.test.id else '-'
                        specification: if report.test? then report.specification.sname else '-'
                        testCaseName: if report.test? then report.test.sname else '-'
                        testSuiteName: if report.testSuite? then report.testSuite.sname else '-'
                        actor: if report.actor? then report.actor.name else '-'
                        startTime: report.result.startTime
                        endTime: if report.result.endTime? then report.result.endTime else '-'
                        result: report.result.result
                        sessionId: report.result.sessionId
                        obsolete: report.result.obsolete
                      transformedObject
      @testResults = resultReportsCollection.value()
      @setPaginationStatus()      
      @refreshCompletedPending = false
      @setFilterRefreshState()
      @completedDataStatus.status = @Constants.STATUS.FINISHED
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @refreshCompletedPending = false
      @setFilterRefreshState()
      @completedDataStatus.status = @Constants.STATUS.FINISHED

  exportVisible: (session) =>
    !session.obsolete? || !session.obsolete

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

  queryDatabase: () =>
    @getActiveTests()
    @getTestResults()

  setPaginationStatus: () =>
    if @currentPage == 1
      @hasNextPage = @testResultsCount > @limit
      @hasPreviousPage = false
    else if @currentPage == Math.ceil(@testResultsCount / @limit)
      @hasNextPage = false
      @hasPreviousPage = true
    else
      @hasNextPage = true
      @hasPreviousPage = true

  rowStyle: (row) => 
    if row.obsolete
      "test-result-obsolete"
    else  
      ""

  onReportExport: (data) =>
    if (!data.obsolete)
      @export = true
      data.exportPending = true
      @ReportService.exportTestCaseReport(data.sessionId, data.testCaseId)
      .then (stepResults) =>
          blobData = new Blob([stepResults], {type: 'application/pdf'});
          saveAs(blobData, "report.pdf");
          data.exportPending = false
      .catch (error) =>
          data.exportPending = false
          @ErrorService.showErrorMessage(error)

  exportActiveSessionsToCsv: () =>
    @exportActivePending = true
    params = @getCurrentSearchCriteria()

    @ReportService.getSystemActiveTestResults(@systemId, params.specIds, params.actorIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.startTimeBeginStr, params.startTimeEndStr, params.sessionId, params.activeSortColumn, params.activeSortOrder)
    .then (data) =>
      resultReportsCollection = _ data.data
      resultReportsCollection = resultReportsCollection
                    .map (report) =>
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
      @exportActivePending = false
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @exportActivePending = false

  exportToCsv: () =>
    @exportCompletedPending = true
    params = @getCurrentSearchCriteria()

    @ReportService.getTestResults(params.systemId, 1, 1000000, params.specIds, params.actorIds, params.testSuiteIds, params.testCaseIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr, params.sessionId, params.sortColumn, params.sortOrder)
    .then (data) =>
      resultReportsCollection = _ data.data
      resultReportsCollection = resultReportsCollection
                    .map (report) =>
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
      @exportCompletedPending = false
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @exportCompletedPending = false

  canDelete: () =>
    !@DataService.isVendorUser

  stopSession: (session) =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you certain you want to terminate this session?", "Yes", "No")
    .then () =>
      session.deletePending = true
      @TestService.stop(session.sessionId)
      .then (data) =>
        session.deletePending = false
        @$state.go @$state.current, {}, {reload: true}
        @PopupService.success('Test session terminated.')
      .catch (error) =>
        session.deletePending = false
        @ErrorService.showErrorMessage(error)

  deleteObsolete: () =>
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

  showCollapseAll: () =>
    @completedExpandedCounter.count > 0

  showCollapseAllActive: () =>
    @activeExpandedCounter.count > 0

  onCollapseAll: () =>
    for test in @testResults
      test.expanded = false
    @completedExpandedCounter.count = 0

  onCollapseAllActive: () =>
    for test in @activeTests
      test.expanded = false
    @activeExpandedCounter.count = 0

@controllers.controller 'SystemTestsController', SystemTestsController