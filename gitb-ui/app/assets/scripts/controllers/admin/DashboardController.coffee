class DashboardController

  @$inject = ['$state', 'CommunityService', 'TestService', 'DataService', 'ReportService', 'Constants', 'SystemConfigurationService', 'PopupService', 'ConfirmationDialogService', 'SpecificationService', 'ConformanceService', 'TestSuiteService', 'OrganizationService', 'SystemService', 'ErrorService']
  constructor: (@$state, @CommunityService, @TestService, @DataService, @ReportService, @Constants, @SystemConfigurationService, @PopupService, @ConfirmationDialogService, @SpecificationService, @ConformanceService, @TestSuiteService, @OrganizationService, @SystemService, @ErrorService) ->

    @exportActivePending = false
    @exportCompletedPending = false
    @viewCheckbox = false
    @selectingForDelete = false
    @activeExpandedCounter = {count: 0}
    @completedExpandedCounter = {count: 0}
    @activeStatus = {status: @Constants.STATUS.PENDING}
    @completedStatus = {status: @Constants.STATUS.PENDING}
    if @DataService.isCommunityAdmin
      @communityId = @DataService.community.id

    @filterState = {
      updatePending: false
    }
    @filters = [@Constants.FILTER_TYPE.SPECIFICATION, @Constants.FILTER_TYPE.ACTOR, @Constants.FILTER_TYPE.TEST_SUITE, @Constants.FILTER_TYPE.TEST_CASE, @Constants.FILTER_TYPE.ORGANISATION, @Constants.FILTER_TYPE.SYSTEM, @Constants.FILTER_TYPE.RESULT, @Constants.FILTER_TYPE.TIME, @Constants.FILTER_TYPE.SESSION, @Constants.FILTER_TYPE.ORGANISATION_PROPERTY, @Constants.FILTER_TYPE.SYSTEM_PROPERTY]
    if @DataService.isSystemAdmin || (@DataService.isCommunityAdmin && !@DataService.community.domain?)
      @filters.push(@Constants.FILTER_TYPE.DOMAIN)
    if @DataService.isSystemAdmin
      @filters.push(@Constants.FILTER_TYPE.COMMUNITY)

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
        title: @DataService.labelOrganisation(),
        sortable: true
      }
      {
        field: 'system',
        title: @DataService.labelSystem(),
        sortable: true
      }
    ]

    @completedTestsColumns = [
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
        title: @DataService.labelOrganisation(),
        sortable: true
      }
      {
        field: 'system',
        title: @DataService.labelSystem(),
        sortable: true
      }
      {
        field: 'result',
        title: 'Result',
        sortable: true,
        iconFn: @DataService.iconForTestResult
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
    @ttlEnabled = false
    @prevParameter = null
    @showFilters = false

    if @DataService.isSystemAdmin
      @SystemConfigurationService.getSessionAliveTime()
      .then (data) =>
        @config = data
        @config.parameter = parseInt(@config.parameter, 10)
        @prevParameter = @config.parameter
        @ttlEnabled = (data.parameter? && !isNaN(data.parameter))
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  getAllCommunities: () =>
    @CommunityService.getCommunities()

  getAllDomains: () =>
    @ConformanceService.getDomains()

  getAllSpecifications: () =>
    if @DataService.isCommunityAdmin && @DataService.community.domainId?
      callResult = @ConformanceService.getSpecifications(@DataService.community.domainId)
    else
      callResult = @ConformanceService.getSpecificationsWithIds()
    callResult

  getAllActors: () =>
    if @DataService.isCommunityAdmin && @DataService.community.domainId?
      callResult = @ConformanceService.getActorsForDomain(@DataService.community.domainId)
    else
      callResult = @ConformanceService.getActorsWithIds()
    callResult

  getAllTestCases: () =>
    if @DataService.isCommunityAdmin && @DataService.community.domainId
      result = @ReportService.getTestCasesForCommunity()
    else
      result = @ReportService.getAllTestCases()
    result

  getAllTestSuites: () =>
    if @DataService.isCommunityAdmin && @DataService.community.domainId?
      result = @TestSuiteService.getTestSuitesWithTestCasesForCommunity()
    else
      result = @TestSuiteService.getAllTestSuitesWithTestCases()
    result

  getAllOrganizations: () =>
    if @DataService.isCommunityAdmin
      result = @OrganizationService.getOrganizationsByCommunity(@DataService.community.id)
    else
      result = @OrganizationService.getOrganizations()
    result

  getAllSystems: () =>
    if @DataService.isSystemAdmin
      result = @SystemService.getSystems()
    else
      result = @SystemService.getSystemsByCommunity()
    result

  getOrganisationPropertiesForFiltering: (communityId) =>
    @CommunityService.getOrganisationParameters(communityId, true)

  getSystemPropertiesForFiltering: (communityId) =>
    @CommunityService.getSystemParameters(communityId, true)

  setFilterRefreshState: () =>
    @filterState.updatePending = @refreshActivePending || @refreshCompletedPending

  getCurrentSearchCriteria:() =>
    filters = @filterState.currentFilters()
    searchCriteria = {}
    if @DataService.isCommunityAdmin
      searchCriteria.communityIds = [@DataService.community.id]
      if @DataService.community.domain?
        searchCriteria.domainIds = [@DataService.community.domain.id]
      else
        searchCriteria.domainIds = filters[@Constants.FILTER_TYPE.DOMAIN]
    else
      searchCriteria.communityIds = filters[@Constants.FILTER_TYPE.COMMUNITY]
      searchCriteria.domainIds = filters[@Constants.FILTER_TYPE.DOMAIN]
    searchCriteria.specIds = filters[@Constants.FILTER_TYPE.SPECIFICATION]
    searchCriteria.actorIds = filters[@Constants.FILTER_TYPE.ACTOR]
    searchCriteria.testSuiteIds = filters[@Constants.FILTER_TYPE.TEST_SUITE]
    searchCriteria.testCaseIds = filters[@Constants.FILTER_TYPE.TEST_CASE]
    searchCriteria.organizationIds = filters[@Constants.FILTER_TYPE.ORGANISATION]
    searchCriteria.systemIds = filters[@Constants.FILTER_TYPE.SYSTEM]
    searchCriteria.results = filters[@Constants.FILTER_TYPE.RESULT]
    searchCriteria.startTimeBeginStr = filters.startTimeBeginStr
    searchCriteria.startTimeEndStr = filters.startTimeEndStr
    searchCriteria.endTimeBeginStr = filters.endTimeBeginStr
    searchCriteria.endTimeEndStr = filters.endTimeEndStr
    searchCriteria.sessionId = filters.sessionId
    searchCriteria.organisationProperties = filters.organisationProperties
    searchCriteria.systemProperties = filters.systemProperties

    searchCriteria.activeSortColumn = @activeSortColumn
    searchCriteria.activeSortOrder = @activeSortOrder
    searchCriteria.completedSortColumn = @completedSortColumn
    searchCriteria.completedSortOrder = @completedSortOrder
    searchCriteria.currentPage = @currentPage
    searchCriteria

  getActiveTests: () =>
    params = @getCurrentSearchCriteria()
    @refreshActivePending = true
    @setFilterRefreshState()
    @ReportService.getActiveTestResults(params.communityIds, params.specIds, params.actorIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.startTimeBeginStr, params.startTimeEndStr, params.sessionId, params.organisationProperties, params.systemProperties, params.activeSortColumn, params.activeSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      @activeTests = _.map data.data, (testResult) => testResultMapper(testResult)
      @refreshActivePending = false
      @setFilterRefreshState()
      @activeStatus.status = @Constants.STATUS.FINISHED      
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @refreshActivePending = false
      @setFilterRefreshState()
      @activeStatus.status = @Constants.STATUS.FINISHED      

  getCompletedTests: () =>
    @viewCheckbox = false
    @selectingForDelete = false
    params = @getCurrentSearchCriteria()
    @refreshCompletedPending = true
    @setFilterRefreshState()
    @ReportService.getCompletedTestResults(params.currentPage, @Constants.TABLE_PAGE_SIZE, params.communityIds, params.specIds, params.actorIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr, params.sessionId, params.organisationProperties, params.systemProperties, params.completedSortColumn, params.completedSortOrder)
    .then (data) =>
      testResultMapper = @newTestResult
      @completedTestsTotalCount = data.count
      @completedTests = _.map data.data, (t) => testResultMapper(t)
      @refreshCompletedPending = false
      @updatePagination()      
      @setFilterRefreshState()
      @completedStatus.status = @Constants.STATUS.FINISHED
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @refreshCompletedPending = false
      @setFilterRefreshState()
      @completedStatus.status = @Constants.STATUS.FINISHED

  newTestResult: (testResult, orgParameters, sysParameters) =>
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
    @getCompletedTests()

  stopSession: (session) =>
    @stop = true
    @ConfirmationDialogService.confirm("Confirm delete", "Are you certain you want to terminate this session?", "Yes", "No")
    .then () =>
      session.deletePending = true
      @TestService.stop(session.session)
      .then (data) =>
        session.deletePending = false
        @$state.go @$state.current, {}, {reload: true}
        @PopupService.success('Test session terminated.')
      .catch (error) =>
        session.deletePending = false
        @ErrorService.showErrorMessage(error)

  queryDatabase: () =>
    @getActiveTests()
    @getCompletedTests()

  goFirstPage: () =>
    @currentPage = 1
    @queryDatabase()

  goPreviousPage: () =>
    @currentPage -= 1
    @queryDatabase()

  goNextPage: () =>
    @currentPage += 1
    @queryDatabase()

  goLastPage: () =>
    @currentPage = Math.ceil(@completedTestsTotalCount / @Constants.TABLE_PAGE_SIZE)
    @queryDatabase()

  updatePagination: () =>
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
    if @config.parameter? && !isNaN(@config.parameter)
      @SystemConfigurationService.updateSessionAliveTime()
      .then (data) =>
        @prevParameter = NaN
        @config.parameter = NaN
        @PopupService.success("Automatic session termination disabled.")
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  apply: () =>
    if @config.parameter? && !isNaN(@config.parameter)
      @SystemConfigurationService.updateSessionAliveTime(@config.parameter)
      .then () =>
        @prevParameter = @config.parameter
        @PopupService.success("Maximum session time set to #{@config.parameter} seconds.")
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @turnOff()
      @ttlEnabled = false

  exportVisible: (session) =>
    !session.obsolete? || !session.obsolete

  onReportExport: (data) =>
    if (!data.obsolete)
      data.exportPending = true
      @ReportService.exportTestCaseReport(data.session, data.testCaseId)
      .then (stepResults) =>
          blobData = new Blob([stepResults], {type: 'application/pdf'});
          saveAs(blobData, "report.pdf");
          data.exportPending = false
      .catch (error) =>
          @ErrorService.showErrorMessage(error)
          data.exportPending = false

  exportCompletedSessionsToCsv: () =>
    @exportCompletedPending = true
    params = @getCurrentSearchCriteria()

    @ReportService.getCompletedTestResults(1, 1000000, params.communityIds, params.specIds, params.actorIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.results, params.startTimeBeginStr, params.startTimeEndStr, params.endTimeBeginStr, params.endTimeEndStr, params.sessionId, params.organisationProperties, params.systemProperties, params.completedSortColumn, params.completedSortOrder, true)
    .then (data) =>
      headers = ["Session", @DataService.labelDomain(), @DataService.labelSpecification(), @DataService.labelActor(), "Test suite", "Test case", @DataService.labelOrganisation()]
      if data.orgParameters?
        for param in data.orgParameters
          headers.push(@DataService.labelOrganisation() + " ("+param+")")
      headers.push(@DataService.labelSystem())
      if data.sysParameters?
        for param in data.sysParameters
          headers.push(@DataService.labelSystem() + " ("+param+")")
      headers = headers.concat(["Start time", "End time", "Result", "Obsolete"])
      testResultMapper = @newTestResult
      tests = _.map data.data, (t) => testResultMapper(t, data.orgParameters, data.sysParameters)
      @DataService.exportAllAsCsv(headers, tests)
      @exportCompletedPending = false
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @exportCompletedPending = false

  exportActiveSessionsToCsv: () =>
    @exportActivePending = true
    params = @getCurrentSearchCriteria()

    @ReportService.getActiveTestResults(params.communityIds, params.specIds, params.actorIds, params.testSuiteIds, params.testCaseIds, params.organizationIds, params.systemIds, params.domainIds, params.startTimeBeginStr, params.startTimeEndStr, params.sessionId, params.organisationProperties, params.systemProperties, params.activeSortColumn, params.activeSortOrder, true)
    .then (data) =>
      headers = ["Session", @DataService.labelDomain(), @DataService.labelSpecification(), @DataService.labelActor(), "Test suite", "Test case", @DataService.labelOrganisation()]
      if data.orgParameters?
        for param in data.orgParameters
          headers.push(@DataService.labelOrganisation() + " ("+param+")")
      headers.push(@DataService.labelSystem())
      if data.sysParameters?
        for param in data.sysParameters
          headers.push(@DataService.labelSystem() + " ("+param+")")
      headers = headers.concat(["Start time", "End time", "Result", "Obsolete"])
      testResultMapper = @newTestResult
      tests = _.map data.data, (testResult) => testResultMapper(testResult, data.orgParameters, data.sysParameters)
      @DataService.exportAllAsCsv(headers, tests)
      @exportActivePending = false
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @exportActivePending = false

  rowStyle: (row) => 
    if row.obsolete
      "test-result-obsolete"
    else  
      ""

  deleteObsolete: () =>
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
        @PopupService.success('Obsolete test results deleted.')
      .catch (error) =>
          @deletePending = false
          @ErrorService.showErrorMessage(error)

  showCollapseAll: () =>
    @completedExpandedCounter.count > 0

  showCollapseAllActive: () =>
    @activeExpandedCounter.count > 0

  onCollapseAll: () =>
    for test in @completedTests
      test.expanded = false
    @completedExpandedCounter.count = 0

  onCollapseAllActive: () =>
    for test in @activeTests
      test.expanded = false
    @activeExpandedCounter.count = 0

  selectDeleteSessions: () =>
    @viewCheckbox = true
    @selectingForDelete = true

  confirmDeleteSessions: () =>
    testsToDelete = []
    for test in @completedTests
      if test.checked? && test.checked
        testsToDelete.push(test.session)
    if testsToDelete.length == 1
      msg = "Are you sure you want to delete the selected test result?"
    else
      msg = "Are you sure you want to delete the selected test results?"
    dialog = @ConfirmationDialogService.confirm("Confirm delete", msg, "Yes", "No")
    dialog.finally(angular.noop).then(
      () => 
        @deleteSessionsPending = true
        @ConformanceService.deleteTestResults(testsToDelete)
        .then () =>
          @deleteSessionsPending = false
          @PopupService.success('Test results deleted.')
          @queryDatabase()
        .catch (error) =>
            @deleteSessionsPending = false
            @viewCheckbox = false
            @selectingForDelete = false
            @ErrorService.showErrorMessage(error)
      , 
      () => 
        @cancelDeleteSessions()
      ,
    )

  cancelDeleteSessions: () =>
    @viewCheckbox = false
    @selectingForDelete = false
    for test in @completedTests
      test.checked = false

  testsChecked: () =>
    for test in @completedTests
      if test.checked? && test.checked
        return true
    false

@controllers.controller 'DashboardController', DashboardController
