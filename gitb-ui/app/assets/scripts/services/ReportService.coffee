class ReportService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService', 'DataService']
  constructor: (@$log, @RestService, @DataService) ->
    @$log.debug "Constructing ReportService..."

  getSystemActiveTestResults: (systemId, specIds, actorIds, testSuiteIds, testCaseIds, domainIds, startTimeBegin, startTimeEnd, sessionId, sortColumn, sortOrder) ->
    params = {
        system_id: systemId
    }

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if actorIds? and actorIds.length > 0
      params.actor_ids = actorIds.join ','

    if testSuiteIds? and testSuiteIds.length > 0
      params.test_suite_ids = testSuiteIds.join ','

    if testCaseIds? and testCaseIds.length > 0
      params.test_case_ids = testCaseIds.join ','

    if domainIds? and domainIds.length > 0
      params.domain_ids = domainIds.join ','

    if startTimeBegin
      params.start_time_begin = startTimeBegin

    if startTimeEnd
      params.start_time_end = startTimeEnd

    if sessionId
      params.session_id = sessionId

    if sortColumn
      params.sort_column = sortColumn

    if sortOrder
      params.sort_order = sortOrder

    @RestService.get
      path: jsRoutes.controllers.ReportService.getSystemActiveTestResults().url
      authenticate: true
      params: params

  getTestResults: (systemId, page, limit, specIds, actorIds, testSuiteIds, testCaseIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, sortColumn, sortOrder) ->
    params = {
        system_id: systemId
        page: page
        limit: limit
    }

    if communityIds? and communityIds.length > 0
      params.community_ids = communityIds.join ','

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if actorIds? and actorIds.length > 0
      params.actor_ids = actorIds.join ','

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
    
    if sessionId
      params.session_id = sessionId

    if sortColumn
      params.sort_column = sortColumn

    if sortOrder
      params.sort_order = sortOrder

    @RestService.get
      path: jsRoutes.controllers.ReportService.getTestResults().url
      authenticate: true
      params: params

  getTestResultsCount: (systemId, specIds, actorIds, testSuiteIds, testCaseIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId) ->
    params = {
        system_id: systemId
    }

    if communityIds? and communityIds.length > 0
      params.community_ids = communityIds.join ','

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if actorIds? and actorIds.length > 0
      params.actor_ids = actorIds.join ','

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
    
    if sessionId
      params.session_id = sessionId

    @RestService.get
      path: jsRoutes.controllers.ReportService.getTestResultsCount().url
      authenticate: true
      params: params

  getActiveTestResults: (communityIds, specIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, startTimeBegin, startTimeEnd, sessionId, organisationParameters, systemParameters, sortColumn, sortOrder, forExport) ->
    params = {}

    if communityIds? and communityIds.length > 0
      params.community_ids = communityIds.join ','

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if actorIds? and actorIds.length > 0
      params.actor_ids = actorIds.join ','

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

    if sessionId
      params.session_id = sessionId

    if organisationParameters && organisationParameters.length > 0
      params.org_params = JSON.stringify(organisationParameters)

    if systemParameters && systemParameters.length > 0
      params.sys_params = JSON.stringify(systemParameters)

    if sortColumn
      params.sort_column = sortColumn

    if sortOrder
      params.sort_order = sortOrder

    params.export = forExport? && forExport

    @RestService.get
      path: jsRoutes.controllers.ReportService.getActiveTestResults().url
      authenticate: true
      params: params

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

  getTestStepReport: (session, reportPath) ->
    #paths like 6[2].1.xml must be escaped
    if (reportPath?) 
      reportPath = reportPath.replace(/\[/g, '__SQS__')
      reportPath = reportPath.replace(/\]/g, '__SQE__')

    @RestService.get
      path: jsRoutes.controllers.RepositoryService.getTestStepReport(session, escape(reportPath)).url
      authenticate: true

  exportConformanceStatementReport: (actorId, systemId, includeTests) ->
      @RestService.get
        path: jsRoutes.controllers.RepositoryService.exportConformanceStatementReport().url
        params:
            actor_id:  actorId
            system_id: systemId
            tests: includeTests 
        authenticate: true
        responseType: "arraybuffer"

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

  exportTestStepReport: (sessionId, reportPath) ->
    @RestService.get
      path: jsRoutes.controllers.RepositoryService.exportTestStepReport(sessionId, reportPath).url
      authenticate: true
      responseType: "arraybuffer"

  getTestCasesForSystem: (systemId) ->
    @RestService.get
      path: jsRoutes.controllers.RepositoryService.getTestCasesForSystem(systemId).url
      authenticate: true

  getAllTestCases: () =>
    @RestService.get
      path: jsRoutes.controllers.RepositoryService.getAllTestCases().url
      authenticate: true

  getTestCasesForCommunity: () =>
    @RestService.get
      path: jsRoutes.controllers.RepositoryService.getTestCasesForCommunity(@DataService.community.id).url
      authenticate: true

  getCompletedTestResults: (page, limit, communityIds, specIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, organisationParameters, systemParameters, sortColumn, sortOrder, forExport) ->
    params = {
        page: page
        limit: limit
    }
    data = {
    }

    if communityIds? and communityIds.length > 0
      params.community_ids = communityIds.join ','

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if actorIds? and actorIds.length > 0
      params.actor_ids = actorIds.join ','

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

    if sessionId
      params.session_id = sessionId
    
    if organisationParameters && organisationParameters.length > 0
      params.org_params = JSON.stringify(organisationParameters)

    if systemParameters && systemParameters.length > 0
      params.sys_params = JSON.stringify(systemParameters)

    params.export = forExport? && forExport

    @RestService.get
      path: jsRoutes.controllers.ReportService.getFinishedTestResults().url
      authenticate: true
      params: params

  getCompletedTestResultsCount: (communityIds, specIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, domainIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, organisationParameters, systemParameters) ->
    params = {}

    if communityIds? and communityIds.length > 0
      params.community_ids = communityIds.join ','

    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','

    if actorIds? and actorIds.length > 0
      params.actor_ids = actorIds.join ','

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

    if sessionId
      params.session_id = sessionId

    if organisationParameters && organisationParameters.length > 0
      params.org_params = JSON.stringify(organisationParameters)

    if systemParameters && systemParameters.length > 0
      params.sys_params = JSON.stringify(systemParameters)

    @RestService.get
      path: jsRoutes.controllers.ReportService.getFinishedTestResultsCount().url
      authenticate: true
      params: params

services.service('ReportService', ReportService)