class DashboardController

  @$inject = ['$log', '$state', '$q', 'CommunityService', 'TestService', 'DataService', 'ReportService', 'Constants', 'SystemConfigurationService', 'PopupService', 'ConfirmationDialogService', 'SpecificationService', 'MessageService', 'ConformanceService', 'TestSuiteService', 'OrganizationService', 'SystemService', 'ErrorService']
  constructor: (@$log, @$state, @$q, @CommunityService, @TestService, @DataService, @ReportService, @Constants, @SystemConfigurationService, @PopupService, @ConfirmationDialogService, @SpecificationService, @MessageService, @ConformanceService, @TestSuiteService, @OrganizationService, @SystemService, @ErrorService) ->

    @activeTestsColumns = [
      {
        field: 'specification',
        title: 'Specification',
        sortable: true
      }
      {
        field: 'actor',
        title: 'Actor',
        sortable: true
      }
      {
        field: 'testCase',
        title: 'Test case',
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
        title: 'Organisation',
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
        field: 'specification',
        title: 'Specification',
        sortable: true
      }
      {
        field: 'actor',
        title: 'Actor',
        sortable: true
      }
      {
        field: 'testCase',
        title: 'Test case',
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
        title: 'Organisation',
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

    if @DataService.isSystemAdmin
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
      @resetFilters(@applySavedSearchState())
      @goPage()
      @getActiveTests()

  applySavedSearchState: () =>
    if @DataService.searchState? && @DataService.searchState.data? && @DataService.searchState.origin == @Constants.SEARCH_STATE_ORIGIN.DASHBOARD
      @savedSearchState = @DataService.searchState.data
      if @savedSearchState.domainIds?
        @filters.domain.selection = _.map(_.filter(@filters.domain.all, (d) => @savedSearchState.domainIds.includes(d.id)), _.clone)
      if @savedSearchState.specIds?
        @filters.specification.selection = _.map(_.filter(@filters.specification.all, (d) => @savedSearchState.specIds.includes(d.id)), _.clone)
      if @savedSearchState.testSuiteIds?
        @filters.testSuite.selection = _.map(_.filter(@filters.testSuite.all, (d) => @savedSearchState.testSuiteIds.includes(d.id)), _.clone)
      if @savedSearchState.testCaseIds?
        @filters.testCase.selection = _.map(_.filter(@filters.testCase.all, (d) => @savedSearchState.testCaseIds.includes(d.id)), _.clone)
      if @savedSearchState.communityIds?
        @filters.community.selection = _.map(_.filter(@filters.community.all, (d) => @savedSearchState.communityIds.includes(d.id)), _.clone)
      if @savedSearchState.organizationIds?
        @filters.organization.selection = _.map(_.filter(@filters.organization.all, (d) => @savedSearchState.organizationIds.includes(d.id)), _.clone)
      if @savedSearchState.systemIds?
        @filters.system.selection = _.map(_.filter(@filters.system.all, (d) => @savedSearchState.systemIds.includes(d.id)), _.clone)
      if @savedSearchState.results?
        @filters.result.selection = _.map(_.filter(@filters.result.all, (d) => @savedSearchState.results.includes(d.result)), _.clone)
        for f in @filters.result.filter
          found = _.find @filters.result.selection, (d) => `d.result == f.result`
          if found?
            f.ticked = true
      @startTime.startDate = @savedSearchState.startTimeBegin
      @startTime.endDate = @savedSearchState.startTimeEnd
      @endTime.startDate = @savedSearchState.endTimeBegin
      @endTime.endDate = @savedSearchState.endTimeEnd
      @currentPage = @savedSearchState.currentPage
      @activeSortColumn = @savedSearchState.activeSortColumn
      @activeSortOrder = @savedSearchState.activeSortOrder
      for column in @activeTestsColumns
        if column.field == @activeSortColumn
          column.order = @activeSortOrder
        else
          column.order = undefined
      @completedSortColumn = @savedSearchState.completedSortColumn
      @completedSortOrder = @savedSearchState.completedSortOrder
      for column in @completedTestsColumns
        if column.field == @completedSortColumn
          column.order = @completedSortOrder
        else
          column.order = undefined
      @showFilters = @savedSearchState.showFilters
      @DataService.clearSearchState()
      @searchState = undefined
      true
    else
      false

  resetFilters: (keepTick) ->
    @setDomainFilter()
    @setCommunityFilter()
    if keepTick
      @setSpecificationFilter(@filters.domain.selection, @filters.domain.filter, keepTick)
      @setTestSuiteFilter(@filters.specification.selection, @filters.specification.filter, keepTick)
      @setTestCaseFilter(@filters.testSuite.selection, @filters.testSuite.filter, keepTick)
      @setOrganizationFilter(@filters.community.selection, @filters.community.filter, keepTick)
      @setSystemFilter(@filters.organization.selection, @filters.organization.filter, keepTick)
    else
      @setSpecificationFilter(@filters.domain.filter, [], keepTick)
      @setTestSuiteFilter(@filters.specification.filter, [], keepTick)
      @setTestCaseFilter(@filters.testSuite.filter, [], keepTick)
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
      if @filters.domain.selection.length > 0
        for f in @filters.domain.filter
          found = _.find @filters.domain.selection, (d) => `d.id == f.id`
          if found?
            f.ticked = true

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
      else
        found.ticked = true

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
      else
        found.ticked = true

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
      else
        found.ticked = true

  setCommunityFilter: () ->
    if @DataService.isCommunityAdmin
      id = @DataService.community.id
      @filters.community.filter = _.map(_.filter(@filters.community.all, (c) => `c.id == id`), _.clone)
      @filters.community.filter[0].ticked = true
      @filters.community.selection = _.map(@filters.community.filter, _.clone)
    else
      @filters.community.filter = _.map(@filters.community.all, _.clone)
      if @filters.community.selection.length > 0
        for f in @filters.community.filter
          found = _.find @filters.community.selection, (c) => `c.id == f.id`
          if found?
            f.ticked = true

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
      else
        found.ticked = true

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
      else
        found.ticked = true

  keepTickedProperty: (oldArr, newArr) ->
    if oldArr? and oldArr.length > 0
      for o, i in newArr
        n = _.find oldArr, (s) => `s.id == o.id`
        o.ticked = if n?.ticked? then n.ticked else false

  getAllCommunities: () ->
    d = @$q.defer()
    if @DataService.isCommunityAdmin
      communityIds = [@DataService.community.id]
    @CommunityService.getCommunities(communityIds)
    .then (data) =>
        @filters.community.all = data
        d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllDomains: () ->
    d = @$q.defer()
    if @DataService.isCommunityAdmin && @DataService.community.domainId?
      domainIds = [@DataService.community.domainId]
    @ConformanceService.getDomains(domainIds)
    .then (data) =>
      @filters.domain.all = data
      d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllSpecifications: () ->
    d = @$q.defer()
    if @DataService.isCommunityAdmin && @DataService.community.domainId?
      callResult = @ConformanceService.getSpecifications(@DataService.community.domainId)
    else
      callResult = @ConformanceService.getSpecificationsWithIds()
    callResult.then (data) =>
       @filters.specification.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllTestCases: () ->
    if @DataService.isCommunityAdmin && @DataService.community.domainId
      tcFunction = @ReportService.getTestCasesForCommunity
    else
      tcFunction = @ReportService.getAllTestCases
    d = @$q.defer()
    tcFunction()
    .then (data) =>
       @filters.testCase.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllTestSuites: () ->
    if @DataService.isCommunityAdmin && @DataService.community.domainId
      tsFunction = @TestSuiteService.getTestSuitesWithTestCasesForCommunity
    else
      tsFunction = @TestSuiteService.getAllTestSuitesWithTestCases
    d = @$q.defer()
    tsFunction()
    .then (data) =>
       @filters.testSuite.all = data
       d.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    d.promise

  getAllOrganizations: () ->
    d = @$q.defer()
    if @DataService.isCommunityAdmin
      @OrganizationService.getOrganizationsByCommunity(@DataService.community.id)
      .then (data) =>
        @filters.organization.all = data
        d.resolve()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @OrganizationService.getOrganizations()
      .then (data) =>
        @filters.organization.all = data
        d.resolve()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    d.promise

  getAllSystems: () ->
    d = @$q.defer()
    if @DataService.isSystemAdmin
      sFunction = @SystemService.getSystems
    else
      sFunction = @SystemService.getSystemsByCommunity
    sFunction().then (data) =>
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

  getCurrentSearchCriteria:() ->
    searchCriteria = {}
    searchCriteria.domainIds = _.map @filters.domain.selection, (s) -> s.id
    searchCriteria.specIds = _.map @filters.specification.selection, (s) -> s.id
    searchCriteria.testSuiteIds = _.map @filters.testSuite.selection, (s) -> s.id
    searchCriteria.testCaseIds = _.map @filters.testCase.selection, (s) -> s.id
    searchCriteria.communityIds = _.map @filters.community.selection, (s) -> s.id
    searchCriteria.organizationIds = _.map @filters.organization.selection, (s) -> s.id
    searchCriteria.systemIds = _.map @filters.system.selection, (s) -> s.id
    searchCriteria.results = _.map @filters.result.selection, (s) -> s.result
    searchCriteria.startTimeBegin = @startTime.startDate
    searchCriteria.startTimeBeginStr = @startTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    searchCriteria.startTimeEnd = @startTime.endDate
    searchCriteria.startTimeEndStr = @startTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    searchCriteria.endTimeBegin = @endTime.startDate
    searchCriteria.endTimeBeginStr = @endTime.startDate?.format('DD-MM-YYYY HH:mm:ss')
    searchCriteria.endTimeEnd = @endTime.endDate
    searchCriteria.endTimeEndStr = @endTime.endDate?.format('DD-MM-YYYY HH:mm:ss')
    searchCriteria.activeSortColumn = @activeSortColumn
    searchCriteria.activeSortOrder = @activeSortOrder
    searchCriteria.completedSortColumn = @completedSortColumn
    searchCriteria.completedSortOrder = @completedSortOrder
    searchCriteria.currentPage = @currentPage
    searchCriteria.showFilters = @showFilters
    searchCriteria

  getActiveTests: () ->
    params = @getCurrentSearchCriteria()

    @ReportService.getActiveTestResults(params.communityIds, params.specIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.startTimeBeginStr, params.startTimeEndStr, params.activeSortColumn, params.activeSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      @activeTests = _.map data.data, (testResult) -> testResultMapper(testResult)
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
    params = @getCurrentSearchCriteria()

    @ReportService.getCompletedTestResultsCount(params.communityIds, params.specIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr)
    .then (data) =>
      @completedTestsTotalCount = data.count
    .then () =>
      @updatePagination()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  getCompletedTests: () =>
    params = @getCurrentSearchCriteria()

    @ReportService.getCompletedTestResults(params.currentPage, @Constants.TABLE_PAGE_SIZE, params.communityIds, params.specIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr, params.completedSortColumn, params.completedSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      @completedTests = _.map data.data, (t) -> testResultMapper(t)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  newTestResult: (testResult, orgParameters, sysParameters) ->
    result = {}
    result.session = testResult.result.sessionId
    result.domain = testResult.domain?.sname
    result.specification = testResult.specification?.sname
    result.actor = testResult.actor?.name
    result.testSuite = testResult.testSuite?.sname
    result.testCase = testResult.test?.sname
    result.organization = testResult.organization?.sname
    if orgParameters?
      for param in orgParameters
        result['organization_'+param] = testResult.organization?.parameters?[param]
    result.system = testResult.system?.sname
    if sysParameters?
      for param in sysParameters
        result['system_'+param] = testResult.system?.parameters?[param]
    result.startTime = testResult.result.startTime
    result.endTime = testResult.result.endTime
    result.result = testResult.result.result
    result.obsolete = testResult.result.obsolete
    result

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
    searchState = @getCurrentSearchCriteria()
    @DataService.setSearchState(searchState, @Constants.SEARCH_STATE_ORIGIN.DASHBOARD)
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
      # if test.domain? and test.specification? and test.testCase? and test.testSuite?
        data = [
          {label: "Domain", value: test.domain}
          {label: "Actor", value: test.actor}
          {label: "Specification", value: test.specification}
          {label: "Test suite", value: test.testSuite}
          {label: "Test case", value: test.testCase}
        ]
        @PopupService.show("Session #{test.session}", data)

  exportCompletedSessionsToCsv: () =>
    params = @getCurrentSearchCriteria()

    @ReportService.getCompletedTestResults(1, 1000000, params.communityIds, params.specIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr, params.completedSortColumn, params.completedSortOrder, true)
    .then (data) =>
      headers = ["Session", "Domain", "Specification", "Actor", "Test suite", "Test case", "Organisation"]
      if data.orgParameters?
        for param in data.orgParameters
          headers.push("Organisation ("+param+")")
      headers.push("System")
      if data.sysParameters?
        for param in data.sysParameters
          headers.push("System ("+param+")")
      headers = headers.concat(["Start time", "End time", "Result", "Obsolete"])
      testResultMapper = @newTestResult
      tests = _.map data.data, (t) -> testResultMapper(t, data.orgParameters, data.sysParameters)
      @DataService.exportAllAsCsv(headers, tests)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  exportActiveSessionsToCsv: () =>
    params = @getCurrentSearchCriteria()

    @ReportService.getActiveTestResults(params.communityIds, params.specIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.startTimeBeginStr, params.startTimeEndStr, params.activeSortColumn, params.activeSortOrder, true)
    .then (data) =>
      headers = ["Session", "Domain", "Specification", "Actor", "Test suite", "Test case", "Organisation"]
      if data.orgParameters?
        for param in data.orgParameters
          headers.push("Organisation ("+param+")")
      headers.push("System")
      if data.sysParameters?
        for param in data.sysParameters
          headers.push("System ("+param+")")
      headers = headers.concat(["Start time", "End time", "Result", "Obsolete"])
      testResultMapper = @newTestResult
      tests = _.map data.data, (testResult) -> testResultMapper(testResult, data.orgParameters, data.sysParameters)
      @DataService.exportAllAsCsv(headers, tests)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  rowStyle: (row) => 
    if row.obsolete
      "test-result-obsolete"
    else  
      ""

  deleteObsolete: () ->
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete all obsolete test results?", "Yes", "No")
    .then () =>
      @deletePending = true
      if @DataService.isCommunityAdmin and @DataService.community.id?
        promise = @ConformanceService.deleteObsoleteTestResultsForCommunity(@DataService.community.id)
      else 
        promise = @ConformanceService.deleteObsoleteTestResults()
      promise.then () =>
        @deletePending = false
        @$state.go @$state.current, {}, {reload: true}
      .catch (error) =>
          @deletePending = false
          @ErrorService.showErrorMessage(error)

@controllers.controller 'DashboardController', DashboardController
