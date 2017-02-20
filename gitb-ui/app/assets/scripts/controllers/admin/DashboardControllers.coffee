class DashboardController
  name: 'DashboardController'

  @$inject = ['$log', 'ReportService', 'Constants', 'PopupService', 'SpecificationService', 'ErrorService']
  constructor: (@$log, @ReportService, @Constants, @PopupService, @SpecificationService, @ErrorService) ->

    # active sessions table
    @activeSessionsColumns = [
      {
        field: 'session',
        title: 'Session'
      }
      {
        field: 'start',
        title: 'Start time (UTC)'
      }
      {
        field: 'organization',
        title: 'Organization'
      }
      {
        field: 'system',
        title: 'System'
      }
    ]

    # completed test sessions
    @completedSessionsColumns = [
      {
        field: 'session',
        title: 'Session'
      }
      {
        field: 'start',
        title: 'Start time (UTC)'
      }
      {
        field: 'end',
        title: 'End time (UTC)'
      }
      {
        field: 'organization',
        title: 'Organization'
      }
      {
        field: 'system',
        title: 'System'
      }
      {
        field: 'result',
        title: 'Result'
      }
    ]

    @activeSessions = []
    @completedSessions = []
    @page = 0
    @count = 0
    @prevDisabled = false
    @nextDisabled = false

    # get active sessions
    @ReportService.getActiveTestResults()
    .then (data) =>
      for session in data
        @activeSessions.push(@getSession(session))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get active sessions
    @ReportService.getCompletedTestResultCount()
    .then (data) =>
      @count = data[0].count
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get completed sessions
    @getCompletedTestResults(@page)
    # set disabled status
    @setDisabledStatus()

  # get completed sessions
  getCompletedTestResults: (page) =>
    @ReportService.getCompletedTestResults(page, @Constants.LIMIT)
    .then (data) =>
      @processRequest(data)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # process data
  processRequest: (data) ->
    @completedSessions = []
    for session in data
      @completedSessions.push(@getSession(session))

  # map session to match table columns
  getSession: (session) ->
    session: session.result.sessionId
    start: session.result.startTime
    end: session.result.endTime
    organization: session.organization?.fname
    system: session.system?.fname
    result: session.result.result
    domain: session.domain?.sname
    specification: session.specification?.sname
    testCase: session.test?.sname
    testCasePath: session.test?.path

  testSelect: (test) =>
    if test.domain? and test.system? and test.specification? and test.testCase? and test.testCasePath?
      data = [{label: "Domain", value: test.domain}
        {label: "System", value: test.system}
        {label: "Specification", value: test.specification}
        {label: "Test case", value: test.testCase}
        {label: "Path", value: test.testCasePath}]
      @PopupService.show("Session #{test.session}", data)

  getSpecification: (id) ->
    spec = {}
    @SpecificationService.getSpecificationById(id)
    .then (data) =>
      spec = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    spec

  firstPage: () =>
    if @page > 0
      @page = 0
      @getCompletedTestResults(@page)
      @setDisabledStatus()

  prevPage: () =>
    if @page > 0
      @getCompletedTestResults(--@page)
      @setDisabledStatus()

  nextPage: () =>
    if (@page + 1) * @Constants.LIMIT < @count
      @getCompletedTestResults(++@page)
      @setDisabledStatus()

  lastPage: () =>
    @page = Math.floor(@count / @Constants.LIMIT) # floor because paging is 0 based
    @getCompletedTestResults(@page)
    @setDisabledStatus()

  setDisabledStatus: () ->
    if @page == 0 # first page
      @nextDisabled = false
      @prevDisabled = true
    else if @page == Math.floor(@count / @Constants.LIMIT) # last page
      @nextDisabled = true
      @prevDisabled = false
    else # pages in between
      @nextDisabled = false
      @prevDisabled = false

@ControllerUtils.register @controllers, DashboardController