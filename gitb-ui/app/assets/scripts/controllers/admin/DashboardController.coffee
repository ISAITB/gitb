class DashboardController

  @$inject = ['$log', '$state', 'TestService', 'ReportService', 'Constants', 'SystemConfigurationService', 'PopupService', 'ConfirmationDialogService', 'SpecificationService', 'MessageService', 'ErrorService']
  constructor: (@$log, @$state, @TestService, @ReportService, @Constants, @SystemConfigurationService, @PopupService, @ConfirmationDialogService, @SpecificationService, @MessageService, @ErrorService) ->

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
    @action = false
    @stop = false
    @config = {}
    @onOff = false
    @prevParameter

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

    @SystemConfigurationService.getSessionAliveTime()
    .then (data) =>
      @config = data
      @config.parameter = parseInt(@config.parameter, 10)
      @prevParameter = @config.parameter
      @onOff = !(data.parameter? && !isNaN(data.parameter))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # get completed sessions
  getCompletedTestResults: (page) =>
    @ReportService.getCompletedTestResults(page, @Constants.TABLE_PAGE_SIZE)
    .then (data) =>
      @processRequest(data)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # turn session alive time parameter off
  turnOff: () =>
    @SystemConfigurationService.updateSessionAliveTime()
    .then (data) =>
      @config.parameter = NaN
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # apply new parameter
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
    if (@page + 1) * @Constants.TABLE_PAGE_SIZE < @count
      @getCompletedTestResults(++@page)
      @setDisabledStatus()

  lastPage: () =>
    @page = Math.floor(@count / @Constants.TABLE_PAGE_SIZE) # floor because paging is 0 based
    @getCompletedTestResults(@page)
    @setDisabledStatus()

  setDisabledStatus: () ->
    if @page == 0 # first page
      @nextDisabled = false
      @prevDisabled = true
    else if @page == Math.floor(@count / @Constants.TABLE_PAGE_SIZE) # last page
      @nextDisabled = true
      @prevDisabled = false
    else # pages in between
      @nextDisabled = false
      @prevDisabled = false

@controllers.controller 'DashboardController', DashboardController