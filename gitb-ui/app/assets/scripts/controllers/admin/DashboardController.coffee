class DashboardController

  @$inject = ['$log', '$state', '$q', 'CommunityService', 'TestService', 'DataService', 'ReportService', 'Constants', 'SystemConfigurationService', 'PopupService', 'ConfirmationDialogService', 'SpecificationService', 'MessageService', 'ConformanceService', 'TestSuiteService', 'OrganizationService', 'SystemService', 'ErrorService']
  constructor: (@$log, @$state, @$q, @CommunityService, @TestService, @DataService, @ReportService, @Constants, @SystemConfigurationService, @PopupService, @ConfirmationDialogService, @SpecificationService, @MessageService, @ConformanceService, @TestSuiteService, @OrganizationService, @SystemService, @ErrorService) ->

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
      community :
        all : []
        filter : []
        selection : []
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

    @domainDisabled = false

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

    @SystemConfigurationService.getSessionAliveTime()
    .then (data) =>
      @config = data
      @config.parameter = parseInt(@config.parameter, 10)
      @prevParameter = @config.parameter
      @onOff = !(data.parameter? && !isNaN(data.parameter))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @getAllResults()
    d1 = @getAllCommunities()
    d2 = @getAllDomains()
    d3 = @getAllSpecifications()
    d4 = @getAllTestSuites()
    d5 = @getAllTestCases()
    d6 = @getAllOrganizations()
    d7 = @getAllSystems()

    @$q.all([d1, d2, d3, d4, d5, d6, d7])
    .then () =>
      @resetFilters(false)
      @goFirstPage()
      @getActiveTests()

  resetFilters: (keepTick) ->
    @setDomainFilter()
    @setSpecificationFilter(@filters.domain.filter, [], keepTick)
    @setTestSuiteFilter(@filters.specification.filter, [], keepTick)
    @setTestCaseFilter(@filters.testSuite.filter, [], keepTick)
    @setCommunityFilter()
    @setOrganizationFilter(@filters.community.filter, [], keepTick)
    @setSystemFilter(@filters.organization.filter, [], keepTick)

  setDomainFilter: () ->
    if @DataService.isCommunityAdmin and @DataService.community.domainId?
      id = @DataService.community.domainId
      @filters.domain.filter = _.map(_.filter(@filters.domain.all, (d) => `d.id == id`), _.clone)
      @filters.domain.filter[0].ticked = true
      @filters.domain.selection = _.map(@filters.domain.filter, _.clone)
    else
      @filters.domain.filter = _.map(@filters.domain.all, _.clone)

  setSpecificationFilter: (selection1, selection2, keepTick) ->
    selection = if selection1? and selection1.length > 0 then selection1 else selection2
    copy = _.map(@filters.specification.filter, _.clone)
    @filters.specification.filter = _.map((_.filter @filters.specification.all, (s) => (_.contains (_.map selection, (d) => d.id), s.domain)), _.clone)
    @keepTickedProperty(copy, @filters.specification.filter) if keepTick

    for i in [@filters.specification.selection.length - 1..0] by -1
      some = @filters.specification.selection[i]
      found = _.find @filters.specification.filter, (s) => `s.id == some.id`
      if (!found?)
        @filters.specification.selection.splice(i, 1)

  setTestSuiteFilter: (selection1, selection2, keepTick) ->
    selection = if selection1? and selection1.length > 0 then selection1 else selection2
    copy = _.map(@filters.testSuite.filter, _.clone)
    @filters.testSuite.filter = _.map((_.filter @filters.testSuite.all, (t) => (_.contains (_.map selection, (s) => s.id), t.specification)), _.clone)
    @keepTickedProperty(copy, @filters.testSuite.filter) if keepTick

    for i in [@filters.testSuite.selection.length - 1..0] by -1
      some = @filters.testSuite.selection[i]
      found = _.find @filters.testSuite.filter, (s) => `s.id == some.id`
      if (!found?)
        @filters.testSuite.selection.splice(i, 1)

  setTestCaseFilter: (selection1, selection2, keepTick) ->
    selection = if selection1? and selection1.length > 0 then selection1 else selection2
    copy = _.map(@filters.testCase.filter, _.clone)
    result = []
    for s, i in selection
      for t, i in s.testCases
        found = _.find @filters.testCase.all, (c) => `c.id == t.id`
        result.push found
    @filters.testCase.filter = _.map(result, _.clone)
    @keepTickedProperty(copy, @filters.testCase.filter) if keepTick

    for i in [@filters.testCase.selection.length - 1..0] by -1
      some = @filters.testCase.selection[i]
      found = _.find @filters.testCase.filter, (s) => `s.id == some.id`
      if (!found?)
        @filters.testCase.selection.splice(i, 1)

  setCommunityFilter: () ->
    if @DataService.isCommunityAdmin
      id = @DataService.community.id
      @filters.community.filter = _.map(_.filter(@filters.community.all, (c) => `c.id == id`), _.clone)
      @filters.community.filter[0].ticked = true
      @filters.community.selection = _.map(@filters.community.filter, _.clone)
    else
      @filters.community.filter = _.map(@filters.community.all, _.clone)

  setOrganizationFilter: (selection1, selection2, keepTick) ->
    selection = if selection1? and selection1.length > 0 then selection1 else selection2
    copy = _.map(@filters.organization.filter, _.clone)
    @filters.organization.filter = _.map((_.filter @filters.organization.all, (o) => (_.contains (_.map selection, (s) => s.id), o.community)), _.clone)
    @keepTickedProperty(copy, @filters.organization.filter) if keepTick

    for i in [@filters.organization.selection.length - 1..0] by -1
      some = @filters.organization.selection[i]
      found = _.find @filters.organization.filter, (s) => `s.id == some.id`
      if (!found?)
        @filters.organization.selection.splice(i, 1)

  setSystemFilter: (selection1, selection2, keepTick) ->
    selection = if selection1? and selection1.length > 0 then selection1 else selection2
    copy = _.map(@filters.system.filter, _.clone)
    @filters.system.filter = _.map((_.filter @filters.system.all, (o) => (_.contains (_.map selection, (s) => s.id), o.owner)), _.clone)
    @keepTickedProperty(copy, @filters.system.filter) if keepTick

    for i in [@filters.system.selection.length - 1..0] by -1
      some = @filters.system.selection[i]
      found = _.find @filters.system.filter, (s) => `s.id == some.id`
      if (!found?)
        @filters.system.selection.splice(i, 1)

  keepTickedProperty: (oldArr, newArr) ->
    if oldArr? and oldArr.length > 0
      for o, i in newArr
        n = _.find oldArr, (s) => `s.id == o.id`
        o.ticked = if n?.ticked? then n.ticked else false

  getAllCommunities: () ->
    d = @$q.defer()
    @CommunityService.getCommunities()
    .then (data) =>
        @filters.community.all = data
        d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllDomains: () ->
    d = @$q.defer()
    @ConformanceService.getDomains()
    .then (data) =>
      @filters.domain.all = data
      d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllSpecifications: () ->
    d = @$q.defer()
    @ConformanceService.getSpecificationsWithIds()
    .then (data) =>
       @filters.specification.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllTestCases: () ->
    d = @$q.defer()
    @ReportService.getTestCases()
    .then (data) =>
       @filters.testCase.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllTestSuites: () ->
    d = @$q.defer()
    @TestSuiteService.getTestSuitesWithTestCases()
    .then (data) =>
       @filters.testSuite.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllOrganizations: () ->
    d = @$q.defer()
    @OrganizationService.getOrganizations()
    .then (data) =>
      @filters.organization.all = data
      d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllSystems: () ->
    d = @$q.defer()
    @SystemService.getSystems()
    .then (data) =>
      @filters.system.all = data
      d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  showFilter: () =>
    @showFilters = true
    @goFirstPage()
    @getActiveTests()

  clearFilter: () =>
    @showFilters = false

    @startTime = {}
    @endTime = {}

    @filters.domain.selection = []
    @filters.specification.selection = []
    @filters.testSuite.selection = []
    @filters.testCase.selection = []
    @filters.community.selection = []
    @filters.organization.selection = []
    @filters.system.selection = []
    @filters.result.selection = []

    @resetFilters(false)

    @goFirstPage()
    @getActiveTests()

  getActiveTests: () ->
    domainIds = _.map @filters.domain.selection, (s) -> s.id
    specIds = _.map @filters.specification.selection, (s) -> s.id
    testSuiteIds = _.map @filters.testSuite.selection, (s) -> s.id
    testCaseIds = _.map @filters.testCase.selection, (s) -> s.id
    communityIds = _.map @filters.community.selection, (s) -> s.id
    organizationIds = _.map @filters.organization.selection, (s) -> s.id
    systemIds = _.map @filters.system.selection, (s) -> s.id
    startTimeBegin = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    startTimeEnd = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')

    @ReportService.getActiveTestResults(communityIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, startTimeBegin, startTimeEnd, @activeSortColumn, @activeSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      @activeTests = _.map data, (testResult) -> testResultMapper(testResult)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  domainClicked: (domain) =>
    @setSpecificationFilter(@filters.domain.selection, @filters.domain.filter, true)
    @setTestSuiteFilter(@filters.specification.selection, @filters.specification.filter, true)
    @setTestCaseFilter(@filters.testSuite.selection, @filters.testSuite.filter, true)
    @goFirstPage()
    @getActiveTests()

  specificationClicked: (spec) =>
    @setTestSuiteFilter(@filters.specification.selection, @filters.specification.filter, true)
    @setTestCaseFilter(@filters.testSuite.selection, @filters.testSuite.filter, true)
    @goFirstPage()
    @getActiveTests()

  testSuiteClicked: (testSuite) =>
    @setTestCaseFilter(@filters.testSuite.selection, @filters.testSuite.filter, true)
    @goFirstPage()
    @getActiveTests()

  testCaseClicked: (testCase) =>
    @goFirstPage()
    @getActiveTests()

  communityClicked: (community) =>
    @setOrganizationFilter(@filters.community.selection, @filters.community.filter, true)
    @setSystemFilter(@filters.organization.selection, @filters.organization.filter, true)
    @goFirstPage()
    @getActiveTests()

  organizationClicked: (organization) =>
    @setSystemFilter(@filters.organization.selection, @filters.organization.filter, true)
    @goFirstPage()
    @getActiveTests()

  systemClicked: (system) =>
    @goFirstPage()
    @getActiveTests()

  resultClicked: (result) =>
    @goFirstPage()

  isDomainDisabled: () =>
    @DataService.isCommunityAdmin

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
    communityIds = _.map @filters.community.selection, (s) -> s.id
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

    @ReportService.getCompletedTestResultsCount(communityIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd)
    .then (data) =>
      @completedTestsTotalCount = data.count
    .then () =>
      @updatePagination()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getCompletedTests: () =>
    communityIds = _.map @filters.community.selection, (s) -> s.id
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

    @ReportService.getCompletedTestResults(@currentPage, @Constants.TABLE_PAGE_SIZE, communityIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, @completedSortColumn, @completedSortOrder)
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
    testSuite: testResult.testSuite?.sname

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
      if test.domain? and test.specification? and test.testCase? and test.testSuite?
        data = [{label: "Domain", value: test.domain}
          {label: "Specification", value: test.specification}
          {label: "Test case", value: test.testCase}
          {label: "Test suite", value: test.testSuite}]
        @PopupService.show("Session #{test.session}", data)

  exportCompletedSessionsToCsv: () =>
    communityIds = _.map @filters.community.selection, (s) -> s.id
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

    @ReportService.getCompletedTestResults(1, 1000000, communityIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, @completedSortColumn, @completedSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      tests = _.map data, (t) -> testResultMapper(t)

      @exportAsCsv(["Session", "Start time", "End time", "Organization", "System", "Result", "Domain", "Specification", "Test case", "Test suite"], tests)

  exportActiveSessionsToCsv: () =>
    communityIds = _.map @filters.community.selection, (s) -> s.id
    specIds = _.map @filters.specification.selection, (s) -> s.id
    testSuiteIds = _.map @filters.testSuite.selection, (s) -> s.id
    testCaseIds = _.map @filters.testCase.selection, (s) -> s.id
    organizationIds = _.map @filters.organization.selection, (s) -> s.id
    systemIds = _.map @filters.system.selection, (s) -> s.id
    domainIds = _.map @filters.domain.selection, (s) -> s.id
    startTimeBegin = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    startTimeEnd = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')

    @ReportService.getActiveTestResults(communityIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, startTimeBegin, startTimeEnd, @activeSortColumn, @activeSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      tests = _.map data, (testResult) -> testResultMapper(testResult)

      @exportAsCsv(["Session", "Start time", "End time", "Organization", "System", "Result", "Domain", "Specification", "Test case", "Test suite"], tests)

  exportAsCsv: (header, data) ->
    if data.length > 0
      csv = "data:text/csv;charset=utf-8,"
      csv += header.toString() + "\n"
      for o, i in data
        line = ""
        idx = 0
        for k, v of o
          if idx++ != 0
            line += ","
          line += v
        csv += if i < data.length then line + "\n" else line

      encodedUri = encodeURI csv
      a = window.document.createElement('a')
      a.setAttribute "href", encodedUri
      a.setAttribute "download", "export.csv"
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)

@controllers.controller 'DashboardController', DashboardController
