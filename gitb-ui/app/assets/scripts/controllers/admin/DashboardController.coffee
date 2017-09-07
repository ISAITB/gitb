class DashboardController

  @$inject = ['$log', '$state', 'TestService', 'ReportService', 'Constants', 'SystemConfigurationService', 'PopupService', 'ConfirmationDialogService', 'SpecificationService', 'MessageService', 'ConformanceService', 'TestSuiteService', 'OrganizationService', 'SystemService', 'ErrorService']
  constructor: (@$log, @$state, @TestService, @ReportService, @Constants, @SystemConfigurationService, @PopupService, @ConfirmationDialogService, @SpecificationService, @MessageService, @ConformanceService, @TestSuiteService, @OrganizationService, @SystemService, @ErrorService) ->

    @activeTestsColumns = [
      {
        field: 'session',
        title: 'Session',
        sortable: true
      }
      {
        field: 'startTime',
        title: 'Start time',
        sortable: true
        order: 'asc'
      }
      {
        field: 'organization',
        title: 'Organization',
        sortable: true
      }
      {
        field: 'system',
        title: 'System',
        sortable: true
      }
    ]

    @completedTestsColumns = [
      {
        field: 'session',
        title: 'Session',
        sortable: true
      }
      {
        field: 'startTime',
        title: 'Start time',
        sortable: true
      }
      {
        field: 'endTime',
        title: 'End time',
        sortable: true,
        order: 'desc'
      }
      {
        field: 'organization',
        title: 'Organization',
        sortable: true
      }
      {
        field: 'system',
        title: 'System',
        sortable: true
      }
      {
        field: 'result',
        title: 'Result',
        sortable: true
      }
    ]

    @activeTests = []

    @completedTests = []
    @completedTestsTotalCount = 0

    @activeSortOrder = "asc"
    @activeSortColumn = "startTime"

    @completedSortOrder = "desc"
    @completedSortColumn = "endTime"

    @currentPage = 1
    @isPreviousPageDisabled = false
    @isNextPageDisabled = false

    @action = false
    @stop = false
    @config = {}
    @onOff = false
    @prevParameter = null
    @showFilters = false

    @translation =
      selectAll       : ""
      selectNone      : ""
      reset           : ""
      search          : "Search..."
      nothingSelected : "All"

    @filters =
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
      organization :
        all : []
        filter : []
        selection : []
      system :
        all : []
        filter : []
        selection : []
      result :
        all : []
        filter : []
        selection : []

    @startTime = {}
    @endTime = {}

    @startTimeOptions =
      locale:
        format: "DD-MM-YYYY"
      eventHandlers:
        'apply.daterangepicker': @applyTimeFiltering
        'cancel.daterangepicker': @clearStartTimeFiltering

    @endTimeOptions =
      locale:
        format: "DD-MM-YYYY"
      eventHandlers:
        'apply.daterangepicker': @applyTimeFiltering
        'cancel.daterangepicker': @clearEndTimeFiltering

    @ConformanceService.getDomains()
    .then (data) =>
      @filters.domain.all = data
      @filters.domain.filter = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ConformanceService.getSpecificationsWithIds()
    .then (data) =>
      @filters.specification.all = data
      @filters.specification.filter = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ReportService.getTestCases()
    .then (data) =>
      @filters.testCase.all = data
      @filters.testCase.filter = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @TestSuiteService.getTestSuitesWithTestCases()
    .then (data) =>
      @filters.testSuite.all = data
      @filters.testSuite.filter = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @OrganizationService.getOrganizations()
    .then (data) =>
      @filters.organization.all = data
      @filters.organization.filter = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @SystemService.getSystems()
    .then (data) =>
      @filters.system.all = data
      @filters.system.filter = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @getAllResults()
    @goFirstPage()
    @getActiveTests()

    @SystemConfigurationService.getSessionAliveTime()
    .then (data) =>
      @config = data
      @config.parameter = parseInt(@config.parameter, 10)
      @prevParameter = @config.parameter
      @onOff = !(data.parameter? && !isNaN(data.parameter))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  showFilter: () =>
    @showFilters = true

  clearFilter: () =>
    @showFilters = false
    @filters.domain.filter = @filters.domain.all
    @filters.specification.filter = @filters.specification.all
    @filters.testSuite.filter = @filters.testSuite.all
    @filters.testCase.filter = @filters.testCase.all
    @filters.organization.filter = @filters.organization.all
    @filters.system.filter = @filters.system.all
    @filters.result.filter = @filters.result.all
    @clearTickedProperty @filters.domain.filter
    @clearTickedProperty @filters.specification.filter
    @clearTickedProperty @filters.testSuite.filter
    @clearTickedProperty @filters.testCase.filter
    @clearTickedProperty @filters.organization.filter
    @clearTickedProperty @filters.system.filter
    @clearTickedProperty @filters.result.filter
    @filters.domain.selection = []
    @filters.specification.selection = []
    @filters.testSuite.selection = []
    @filters.testCase.selection = []
    @filters.organization.selection = []
    @filters.system.selection = []
    @filters.result.selection = []
    @startTime = {}
    @endTime = {}
    @goFirstPage()
    @getActiveTests()

  clearTickedProperty: (selection) ->
    for s, i in selection
      s.ticked = false

  getActiveTests: () ->
    specIds = _.map @filters.specification.selection, (s) -> s.id
    testSuiteIds = _.map @filters.testSuite.selection, (s) -> s.id
    testCaseIds = _.map @filters.testCase.selection, (s) -> s.id
    organizationIds = _.map @filters.organization.selection, (s) -> s.id
    systemIds = _.map @filters.system.selection, (s) -> s.id
    domainIds = _.map @filters.domain.selection, (s) -> s.id
    startTimeBegin = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    startTimeEnd = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')

    @ReportService.getActiveTestResults(specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, startTimeBegin, startTimeEnd, @activeSortColumn, @activeSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      @activeTests = _.map data, (testResult) -> testResultMapper(testResult)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  updateFilter: (changedFilter, updateFilter, matchFunction) ->
    result = []
    if @filters[changedFilter].selection.length > 0
      for s, i in @filters[changedFilter].selection
        for v, i in @filters[updateFilter].all
          if matchFunction(s, v)
            found = _.find @filters[updateFilter].filter, (f) =>
              `f.id == v.id`
            v.ticked = if found?.ticked? then found.ticked else false
            result.push v
    else
      result = @filters[updateFilter].all
    @filters[updateFilter].filter = result

  domainClicked: (domain) =>
    @updateFilter 'domain', 'specification', (s, v) -> `s.id == v.domain`
    @updateFilter 'specification', 'testSuite', (s, v) -> `s.id == v.specification`
    @updateFilter 'testSuite', 'testCase', (s, v) -> v.id in (_.map s.testCases, (testCase) -> testCase.id)
    @goFirstPage()
    @getActiveTests()

  specificationClicked: (spec) =>
    @updateFilter 'specification', 'testSuite', (s, v) -> `s.id == v.specification`
    @updateFilter 'testSuite', 'testCase', (s, v) -> v.id in (_.map s.testCases, (testCase) -> testCase.id)
    @goFirstPage()
    @getActiveTests()

  testSuiteClicked: (testSuite) =>
    @updateFilter 'testSuite', 'testCase', (s, v) -> v.id in (_.map s.testCases, (testCase) -> testCase.id)
    @goFirstPage()
    @getActiveTests()

  testCaseClicked: (testCase) =>
    @goFirstPage()
    @getActiveTests()

  organizationClicked: (organization) =>
    @updateFilter 'organization', 'system', (s, v) -> `s.id == v.owner`
    @goFirstPage()
    @getActiveTests()

  systemClicked: (system) =>
    @goFirstPage()
    @getActiveTests()

  resultClicked: (result) =>
    @goFirstPage()

  applyTimeFiltering: (ev, picker) =>
    @goFirstPage()
    @getActiveTests()

  clearTimeFiltering: (time) =>
    time.endDate = null
    time.startDate = null
    @goFirstPage()
    @getActiveTests()

  clearStartTimeFiltering: (ev, picker) =>
    @clearTimeFiltering @startTime

  clearEndTimeFiltering: (ev, picker) =>
    @clearTimeFiltering @endTime

  getAllResults: () ->
    for k, v of @Constants.TEST_CASE_RESULT
      result = {result: v}
      @filters.result.all.push result
      @filters.result.filter.push result

  getTotalTestCount: () ->
    specIds = _.map @filters.specification.selection, (s) -> s.id
    testSuiteIds = _.map @filters.testSuite.selection, (s) -> s.id
    testCaseIds = _.map @filters.testCase.selection, (s) -> s.id
    organizationIds = _.map @filters.organization.selection, (s) -> s.id
    systemIds = _.map @filters.system.selection, (s) -> s.id
    domainIds = _.map @filters.domain.selection, (s) -> s.id
    results = _.map @filters.result.selection, (s) -> s.result
    startTimeBegin = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    startTimeEnd = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeBegin = @endTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeEnd = @endTime.endDate?.format('DD-MM-YYYY HH:mm:ss')

    @ReportService.getCompletedTestResultsCount(specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd)
    .then (data) =>
      @completedTestsTotalCount = data.count
    .then () =>
      @updatePagination()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getCompletedTests: () =>
    specIds = _.map @filters.specification.selection, (s) -> s.id
    testSuiteIds = _.map @filters.testSuite.selection, (s) -> s.id
    testCaseIds = _.map @filters.testCase.selection, (s) -> s.id
    organizationIds = _.map @filters.organization.selection, (s) -> s.id
    systemIds = _.map @filters.system.selection, (s) -> s.id
    domainIds = _.map @filters.domain.selection, (s) -> s.id
    results = _.map @filters.result.selection, (s) -> s.result
    startTimeBegin = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    startTimeEnd = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeBegin = @endTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    endTimeEnd = @endTime.endDate?.format('DD-MM-YYYY HH:mm:ss')

    @ReportService.getCompletedTestResults(@currentPage, @Constants.TABLE_PAGE_SIZE, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, @completedSortColumn, @completedSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      @completedTests = _.map data, (t) -> testResultMapper(t)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  newTestResult: (testResult) ->
    session: testResult.result.sessionId
    startTime: testResult.result.startTime
    endTime: testResult.result.endTime
    organization: testResult.organization?.fname
    system: testResult.system?.fname
    result: testResult.result.result
    domain: testResult.domain?.sname
    specification: testResult.specification?.sname
    testCase: testResult.test?.sname
    testCasePath: testResult.test?.path

  sortActiveSessions: (column) =>
    @activeSortColumn = column.field
    @activeSortOrder = column.order
    @getActiveTests()

  sortCompletedSessions: (column) =>
    @completedSortColumn = column.field
    @completedSortOrder = column.order
    @goPage()

  onAction: (session) =>
    @action = true
    @$state.go 'app.reports.presentation', {session_id: session.session}

  stopSession: (session) =>
    @stop = true
    @ConfirmationDialogService.confirm("Confirm delete", "Are you certain you want to terminate this session?", "Yes", "No")
    .then () =>
      @TestService.stop(session.session)
      .then (data) =>
        @$state.go @$state.current, {}, {reload: true}
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  goFirstPage: () =>
    @currentPage = 1
    @goPage()

  goPreviousPage: () =>
    @currentPage -= 1
    @goPage()

  goNextPage: () =>
    @currentPage += 1
    @goPage()

  goLastPage: () =>
    @currentPage = Math.ceil(@completedTestsTotalCount / @Constants.TABLE_PAGE_SIZE)
    @goPage()

  goPage: () ->
    @getCompletedTests()
    @getTotalTestCount()

  updatePagination: () ->
    if @currentPage == 1
      @isNextPageDisabled = @completedTestsTotalCount <= @Constants.TABLE_PAGE_SIZE
      @isPreviousPageDisabled = true
    else if @currentPage == Math.ceil(@completedTestsTotalCount / @Constants.TABLE_PAGE_SIZE)
      @isNextPageDisabled = true
      @isPreviousPageDisabled = false
    else
      @isNextPageDisabled = false
      @isPreviousPageDisabled = false

  turnOff: () =>
    @SystemConfigurationService.updateSessionAliveTime()
    .then (data) =>
      @prevParameter = NaN
      @config.parameter = NaN
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  apply: () =>
    if @config.parameter? && !isNaN(@config.parameter)
      @SystemConfigurationService.updateSessionAliveTime(@config.parameter)
      .then () =>
        @prevParameter = @config.parameter
        @MessageService.showMessage("Update successful", "Maximum session alive time set to #{@config.parameter} seconds.")
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @turnOff()
      @onOff = true

  testSelect: (test) =>
    if @action
      @action = false
    else if @stop
      @stop = false
    else
      if test.domain? and test.specification? and test.testCase? and test.testCasePath?
        data = [{label: "Domain", value: test.domain}
          {label: "Specification", value: test.specification}
          {label: "Test case", value: test.testCase}
          {label: "Path", value: test.testCasePath}]
        @PopupService.show("Session #{test.session}", data)

@controllers.controller 'DashboardController', DashboardController