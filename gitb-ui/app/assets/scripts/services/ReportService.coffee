class ReportService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  constructor: (@$log, @RestService) ->
    @$log.debug "Constructing ReportService..."

  getTestResults: (systemId, page, limit, specIds, testSuiteIds, testCaseIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder) ->
    params = {
        system_id: systemId
        page: page
        limit: limit
    }

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if testSuiteIds? and testSuiteIds.length > 0
      params.test_suite_ids = testSuiteIds.join ','

    if testCaseIds? and testCaseIds.length > 0
      params.test_case_ids = testCaseIds.join ','

    if domainIds? and domainIds.length > 0
      params.domain_ids = domainIds.join ','

    if results? and results.length > 0
      params.results = results.join ','

    if startTimeBegin
      params.start_time_begin = startTimeBegin

    if startTimeEnd
      params.start_time_end = startTimeEnd

    if endTimeBegin
      params.end_time_begin = endTimeBegin

    if endTimeEnd
      params.end_time_end = endTimeEnd

    if startTimeEnd
      params.start_time_end = startTimeEnd

    if sortColumn
      params.sort_column = sortColumn

    if sortOrder
      params.sort_order = sortOrder

    @RestService.get
      path: jsRoutes.controllers.ReportService.getTestResults().url
      authenticate: true
      params: params

  getTestResultsCount: (systemId, specIds, testSuiteIds, testCaseIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd) ->
    params = {
        system_id: systemId
    }

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if testSuiteIds? and testSuiteIds.length > 0
      params.test_suite_ids = testSuiteIds.join ','

    if testCaseIds? and testCaseIds.length > 0
      params.test_case_ids = testCaseIds.join ','

    if domainIds? and domainIds.length > 0
      params.domain_ids = domainIds.join ','

    if results? and results.length > 0
      params.results = results.join ','

    if startTimeBegin
      params.start_time_begin = startTimeBegin

    if startTimeEnd
      params.start_time_end = startTimeEnd

    if endTimeBegin
      params.end_time_begin = endTimeBegin

    if endTimeEnd
      params.end_time_end = endTimeEnd

    if startTimeEnd
      params.start_time_end = startTimeEnd

    @RestService.get
      path: jsRoutes.controllers.ReportService.getTestResultsCount().url
      authenticate: true
      params: params

  getActiveTestResults: (specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, startTimeBegin, startTimeEnd, sortColumn, sortOrder) ->
    params = {}

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if testSuiteIds? and testSuiteIds.length > 0
      params.test_suite_ids = testSuiteIds.join ','

    if testCaseIds? and testCaseIds.length > 0
      params.test_case_ids = testCaseIds.join ','

    if organizationIds? and organizationIds.length > 0
      params.organization_ids = organizationIds.join ','

    if domainIds? and domainIds.length > 0
      params.domain_ids = domainIds.join ','

    if systemIds? and systemIds.length > 0
      params.system_ids = systemIds.join ','

    if startTimeBegin
      params.start_time_begin = startTimeBegin

    if startTimeEnd
      params.start_time_end = startTimeEnd

    if sortColumn
      params.sort_column = sortColumn

    if sortOrder
      params.sort_order = sortOrder

    @RestService.get
      path: jsRoutes.controllers.ReportService.getActiveTestResults().url
      authenticate: true
      params: params

  getCompletedTestResults: (page, limit) ->
    @RestService.get
      path: jsRoutes.controllers.ReportService.getCompletedTestResults().url
      params:
        page: page
        limit: limit
      authenticate: true

  getCompletedTestResultCount: () ->
    @RestService.get({
      path: jsRoutes.controllers.ReportService.getCompletedTestResultCount().url,
      authenticate: true
    })

  getTestResultOfSession: (sessionId) ->
    @RestService.get({
      path: jsRoutes.controllers.ReportService.getTestResultOfSession(sessionId).url,
      authenticate: true
    })

  createTestReport: (sessionId, systemId, actorId, testId) ->
    @RestService.post({
      path: jsRoutes.controllers.ReportService.createTestReport().url,
      data: {
        session_id: sessionId
        system_id : systemId
        actor_id  : actorId
        test_id   : testId
      },
      authenticate: true
    })

  getTestStepResults: (sessionId) ->
    @RestService.get({
      path: jsRoutes.controllers.ReportService.getTestStepResults(sessionId).url
      authenticate: true
    })

  getTestStepReport: (reportPath) ->
    @RestService.get
      path: jsRoutes.controllers.RepositoryService.getTestStepReport(reportPath).url
      authenticate: true

  exportTestCaseReport: (session, testName) ->
      @RestService.get
        path: jsRoutes.controllers.RepositoryService.exportTestCaseReport().url
        params:
            session_id:  session
            test_id: testName
        authenticate: true
        responseType: "arraybuffer"

  exportTestCaseReports: (session_ids, test_ids) ->
        @RestService.get
          path: jsRoutes.controllers.RepositoryService.exportTestCaseReports().url
          params:
              session_ids:  session_ids
              test_ids: test_ids
          authenticate: true
          responseType: "arraybuffer"

  exportTestStepReport: (reportPath) ->
    @RestService.get
      path: jsRoutes.controllers.RepositoryService.exportTestStepReport(reportPath).url
      authenticate: true
      responseType: "arraybuffer"

  getTestCases: (testCaseIds) ->
    params = {}
    if ids? and testCaseIds.length > 0
        params.ids = testCaseIds.join ','

    @RestService.get
      path: jsRoutes.controllers.RepositoryService.getTestCases().url
      authenticate: true
      params: params

  getCompletedTestResults: (page, limit, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder) ->
    params = {
        page: page
        limit: limit
    }

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if testSuiteIds? and testSuiteIds.length > 0
      params.test_suite_ids = testSuiteIds.join ','

    if testCaseIds? and testCaseIds.length > 0
      params.test_case_ids = testCaseIds.join ','

    if organizationIds? and organizationIds.length > 0
      params.organization_ids = organizationIds.join ','

    if domainIds? and domainIds.length > 0
      params.domain_ids = domainIds.join ','

    if systemIds? and systemIds.length > 0
      params.system_ids = systemIds.join ','

    if results? and results.length > 0
      params.results = results.join ','

    if startTimeBegin
      params.start_time_begin = startTimeBegin

    if startTimeEnd
      params.start_time_end = startTimeEnd

    if endTimeBegin
      params.end_time_begin = endTimeBegin

    if endTimeEnd
      params.end_time_end = endTimeEnd

    if startTimeEnd
      params.start_time_end = startTimeEnd

    if sortColumn
      params.sort_column = sortColumn

    if sortOrder
      params.sort_order = sortOrder

    @RestService.get
      path: jsRoutes.controllers.ReportService.getFinishedTestResults().url
      authenticate: true
      params: params

  getCompletedTestResultsCount: (specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd) ->
    params = {}

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if testSuiteIds? and testSuiteIds.length > 0
      params.test_suite_ids = testSuiteIds.join ','

    if testCaseIds? and testCaseIds.length > 0
      params.test_case_ids = testCaseIds.join ','

    if organizationIds? and organizationIds.length > 0
      params.organization_ids = organizationIds.join ','

    if domainIds? and domainIds.length > 0
      params.domain_ids = domainIds.join ','

    if systemIds? and systemIds.length > 0
      params.system_ids = systemIds.join ','

    if results? and results.length > 0
      params.results = results.join ','

    if startTimeBegin
      params.start_time_begin = startTimeBegin

    if startTimeEnd
      params.start_time_end = startTimeEnd

    if endTimeBegin
      params.end_time_begin = endTimeBegin

    if endTimeEnd
      params.end_time_end = endTimeEnd

    @RestService.get
      path: jsRoutes.controllers.ReportService.getFinishedTestResultsCount().url
      authenticate: true
      params: params

services.service('ReportService', ReportService)