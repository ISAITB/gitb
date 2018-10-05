class TestExecutionControllerV2
  @$inject = ['$log', '$scope', '$location', '$modal', '$state', '$stateParams', 'Constants', 'TestService', 'SystemService', 'ConformanceService', 'WebSocketService', 'ReportService', 'ErrorService', 'DataService', '$q', '$timeout', '$interval']
  constructor: (@$log, @$scope, @$location, @$modal, @$state, @$stateParams, @Constants, @TestService, @SystemService, @ConformanceService, @WebSocketService, @ReportService, @ErrorService, @DataService, @$q, @$timeout, @$interval) ->
    @testsToExecute = @DataService.tests
    if (!@testsToExecute?)
      # We lost our state following a refresh - recreate state.
      testSuiteId = @$stateParams['testSuiteId']
      if (testSuiteId?)
        actorId = @$stateParams['actorId']
        systemId = @$stateParams['systemId']
        @ConformanceService.getConformanceStatusForTestSuite(actorId, systemId, testSuiteId)
          .then((data) =>
              # There will always be one test suite returned
              tests = []
              for result in data
                testCase = {}
                testCase.id = result.testCaseId
                testCase.sname = result.testCaseName
                testCase.description = result.testCaseDescription
                tests.push(testCase)
              @testsToExecute = tests
              @initialiseState()
            (error) =>
              @ErrorService.showErrorMessage(error)
          )
      else
        testCaseId = @$stateParams['testCaseId']
        @ConformanceService.getTestSuiteTestCase(testCaseId)
          .then(
            (data) =>
              # There will always be one test suite returned
              @testsToExecute = [data]
              @initialiseState()
            (error) =>
              @ErrorService.showErrorMessage(error)
          )
    else 
      @initialiseState()

  initialiseState: () =>
    @systemId = parseInt(@$stateParams['systemId'], 10)
    @actorId  = parseInt(@$stateParams['actorId'], 10)
    @specId   = parseInt(@$stateParams['specId'], 10)
    @started	= false #test started or not
    @firstTestStarted	= false
    @reload	  = false
    @startAutomatically	  = false
    @actorConfigurations = {}
    @$scope.interactions  = []
    @selectedInteroperabilitySession = null
    @isOwner = false
    @wizardStep = 0
    @intervalSet = false
    @progressIcons = {}
    @testCaseStatus = {}
    @$scope.stepsOfTests = {}
    @$scope.actorInfoOfTests = {}
    @$scope.$on '$destroy', () =>
      if @ws? and @session?
        @$log.debug 'Closing websocket in $destroy event handler'
        @ws.close()

      return
    @parameterTableColumns = [
      {
        field: 'name'
        title: 'Name'
      }
      {
        field: 'use'
        title: 'Usage'
      }
      {
        field: 'kind'
        title: 'Type'
      }
      {
        field: 'configured'
        title: 'Configured'
      }
    ]
    @sessionTableColumns = [
      {
        title:'Session'
        field:'session'
      }
    ]
    @actorTableColumns = [
      {
        title:'Actor'
        field:'actor'
      }
    ]
    @stopped = false
    @currentTestIndex = 0
    @currentTest = @testsToExecute[@currentTestIndex]
    @visibleTest = @currentTest
    for test in @testsToExecute
      @updateTestCaseStatus(test.id, @Constants.TEST_CASE_STATUS.PENDING)
      @$scope.stepsOfTests[test.id] = {}
      @$scope.actorInfoOfTests[test.id] = {}

    @endpointsLoaded = @$q.defer()
    @configurationsLoaded = @$q.defer()
    @testCaseLoaded = @$q.defer()
    @$q.all(@endpointsLoaded.promise, @configurationsLoaded.promise, @testCaseLoaded.promise).then(() =>
      # Start the wizard
      @nextStep()
    )
    @getEndpointConfigurations(@actorId)
    @getTestCaseDefinition(@currentTest.id)

  stopAll: () =>
    @stopped = true
    if (@session?)
      @stop(@session)
    @started = false 
    @startAutomatically = false
    @reload = true

  tableRowClass: (testCase) =>
    if (@isTestCaseClickable(testCase))
      "clickable-row"
    else if (testCase.id == @visibleTest.id && @testsToExecute.length > 1)
      "selected-row"
    else
      ""

  isTestCaseClickable: (testCase) =>
    testCase.id != @visibleTest.id && @testCaseStatus[testCase.id] != @Constants.TEST_CASE_STATUS.PENDING

  viewTestCase: (testCase) =>
    if (@isTestCaseClickable(testCase))
      @visibleTest = testCase

  updateTestCaseStatus: (testId, status) =>
    @testCaseStatus[testId] = status
    if (status == @Constants.TEST_CASE_STATUS.PROCESSING)
      @progressIcons[testId] = "fa-gear fa-spin test-case-running"
    else if (status == @Constants.TEST_CASE_STATUS.READY)
      @progressIcons[testId] = "fa-gear test-case-ready"
    else if (status == @Constants.TEST_CASE_STATUS.PENDING)
      @progressIcons[testId] = "fa-gear test-case-pending"
    else if (status == @Constants.TEST_CASE_STATUS.ERROR)
      @progressIcons[testId] = "fa-times-circle test-case-error"
    else if (status == @Constants.TEST_CASE_STATUS.COMPLETED)
      @progressIcons[testId] = "fa-check-circle test-case-success"
    else if (status == @Constants.TEST_CASE_STATUS.STOPPED)
      @progressIcons[testId] = "fa-gear test-case-stopped"
    else
      @progressIcons[testId] = "fa-gear test-case-pending"

  progressIcon: (testCaseId) =>
    @progressIcons[testCaseId]

  runInitiateStep:() =>
    @updateTestCaseStatus(@currentTest.id, @Constants.TEST_CASE_STATUS.READY)
    initiateFinished = @$q.defer()
    @initiate(@currentTest.id, initiateFinished)
    initiateFinished.promise.then(() =>
      configsDiffer = true
      if (@currentSimulatedConfigs?)
        configsDiffer = @configsDifferent(@currentSimulatedConfigs, @simulatedConfigs)        
      else
        @currentSimulatedConfigs = @simulatedConfigs
      if (@simulatedConfigs && @simulatedConfigs.length > 0 && configsDiffer)
        @wizardStep = 2
      else
        @wizardStep = 3
        if (@startAutomatically)
          @start(@session)
    )

  configsDifferent: (previous, current) =>
    for c1 in previous
      delete c1["$$hashKey"]
      for c2 in c1.configs
        delete c2["$$hashKey"]
        for c3 in c2.config
          delete c3["$$hashKey"]
    configStr1 = JSON.stringify(previous)
    configStr2 = JSON.stringify(current)
    console.log "HERE"
    !(configStr1 == configStr2)

  nextStep: (stepToConsider) =>
    if (!@stopped)
      if (!stepToConsider?)
        stepToConsider = @wizardStep
      if (stepToConsider == 0)
        # Before starting
        if (!@isSystemConfigurationsValid(@endpointRepresentations))
          @wizardStep = 1
        else 
          @runInitiateStep()
      else if (stepToConsider == 1)
        @runInitiateStep()
      else if (stepToConsider == 2)
        @wizardStep = 3
        if (@startAutomatically)
          @start(@session)
      else if (@wizardStep == 3)
        # We are running the next test case
        @startNextTest()

  startNextTest: () =>
    if (@currentTestIndex + 1 < @testsToExecute.length)
      @currentTestIndex += 1
      @currentTest = @testsToExecute[@currentTestIndex]
      @visibleTest = @currentTest
      @startAutomatically = true
      @testCaseLoaded = @$q.defer()
      @getTestCaseDefinition(@currentTest.id)
      @testCaseLoaded.promise.then(() =>
        @nextStep(1)
      )
    else
      @startAutomatically = false
      @started = false 
      @reload = true

  testCaseFinished: (result) =>
    if (result == "SUCCESS")
      @updateTestCaseStatus(@currentTest.id, @Constants.TEST_CASE_STATUS.COMPLETED)
    else if (result == "FAILURE")
      @updateTestCaseStatus(@currentTest.id, @Constants.TEST_CASE_STATUS.ERROR)
    else
      @updateTestCaseStatus(@currentTest.id, @Constants.TEST_CASE_STATUS.STOPPED)
    if (@currentTestIndex + 1 < @testsToExecute.length)
      @$timeout(() => 
        @nextStep()
      , 1000)
    else
      @nextStep()

  getEndpointConfigurations :(actorId)->
    @ConformanceService.getEndpointsForActor actorId
        .then (endpoints) =>
          @endpoints = endpoints
        .then () =>
          if @endpoints?.length > 0
            endpointIds = _.map @endpoints, (endpoint) -> endpoint.id
            @SystemService.getConfigurationsWithEndpointIds(endpointIds, @systemId)
            .then (configurations) =>
              @configurations = configurations
              @constructEndpointRepresentations()
              @configurationsLoaded.resolve()

              for configuration in @configurations
                endpoint = @getEndpointForId(configuration.endpoint)
                for parameter in endpoint.parameters
                  if parameter.id == configuration.parameter
                    parameter.value = configuration.value
              @endpointsLoaded.resolve()
          else
            @endpointsLoaded.resolve()
            @configurationsLoaded.resolve()

        .catch (error) =>
          @ErrorService.showErrorMessage(error, true).result.then () =>
            @$state.go @$state.current, {}, {reload: true}

  constructEndpointRepresentations: () =>
    @endpointRepresentations = _.map @endpoints, (endpoint) =>
        name: endpoint.name
        description: endpoint.description
        id: endpoint.id
        parameters: _.map endpoint.parameters, (parameter) =>
          repr = _.cloneDeep parameter
          repr.configured =  _.some @configurations, (configuration) =>
            parameter.id == configuration.parameter &&
              Number(parameter.endpoint) == Number(configuration.endpoint) &&
              configuration.value?
          repr

  isSystemConfigurationsValid: (endpoints) ->
    valid = true

    if endpoints?
      for endpoint in endpoints
        for parameter in endpoint.parameters
          if !parameter.configured && parameter.use == "R"
            valid = false
            break
        if !valid
          break

    valid

  getEndpointForId: (id) ->
    _endpoint = null
    for endpoint in @endpoints
      if endpoint.id == id
        _endpoint = endpoint
        break;
    _endpoint

  getTestCaseDefinition: (testCaseToLookup) ->
    @TestService.getTestCaseDefinition(testCaseToLookup)
    .then(
      (data) =>
        testcase = data
        if (testcase.preliminary?)
          @currentTest["preliminary"] = testcase.preliminary
        
        @TestService.getActorDefinitions(@specId).then((data) =>
          @$scope.actorInfoOfTests[testCaseToLookup] = testcase.actors.actor
          for domainActorData in data
            if (domainActorData.id == @actorId)
              @actor = domainActorData.actorId
            for testCaseActorData in @$scope.actorInfoOfTests[testCaseToLookup]
              if (testCaseActorData.id == domainActorData.actorId)
                if (!(testCaseActorData.name?))
                  testCaseActorData.name = domainActorData.name
                break

          @$scope.stepsOfTests[testCaseToLookup] = testcase.steps
          @$scope.$broadcast('sequence:testLoaded', testCaseToLookup)
          # @$scope.steps = testcase.steps
          @testCaseLoaded.resolve()

          @isInteroperabilityTesting = testcase.metadata.type == @Constants.TEST_CASE_TYPE.INTEROPERABILITY
          if @isInteroperabilityTesting
            @$scope.stepsOfTests[testCaseToLookup].splice(1, 0, {id:2, title:"Participants" })
            @$scope.stepsOfTests[testCaseToLookup].id = 3
            @$scope.stepsOfTests[testCaseToLookup].id = 4

            @getInteroperabilitySessions()

          for testCaseActor in @$scope.actorInfoOfTests[testCaseToLookup]
            if testCaseActor.role == @Constants.TEST_ROLE.SUT
              @actorConfigurations[testCaseActor.id] = null
        ,
        (error) =>
          @ErrorService.showErrorMessage(error, true).result
        )
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  getActorName: (actorId) ->
    actorName = actorId
    for info in @$scope.actorInfoOfTests[@currentTest.id]
      if actorId == info.id
        if info.name?
          actorName = info.name
        else
          actorName = info.id
        break
    actorName

  getInteroperabilitySessions: () ->
    @TestService.getSessions()
    .then(
      (data) =>
        @interoperabilitySessions = data
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  join: () ->
    @session = @selectedSession
    @ws = @WebSocketService.createWebSocket({
      callbacks: {
        onopen : @onopen,
        onclose : @onclose,
        onmessage : @onmessage
      }
    })
    @$scope.$broadcast 'wizard-directive:next'

  createInteroperabilitySession: () ->
    $("#createSession").text("Waiting...").attr("disabled", true)
    $("#joinSession").attr("disabled", true)

    @isOwner = true
    @initiate(@currentTest.id)

  initiate: (testCase, initiateFinished) ->
    @socketOpen = @$q.defer()
    @socketOpen.promise.then(() =>
      if (!@intervalSet)
        @intervalSet = true
        @$interval(@processNextMessage, 100, false)

    )
    @TestService.initiate(testCase)
    .then(
      (data) =>
        @session = data
        @actorConfigurations[@actor] = @createActorConfigurations(@configurations)
        list = _.values(@actorConfigurations)

        #if actor configurations list contains null values, that means other actors of interoperability
        #testing did not send their configurations yet. WON'T happen in Conformance Testing
        if !_.contains(list, null)
          configureFinished = @$q.defer()
          @configure(@session, list, configureFinished)

        #create a WebSocket when a new session is initiated
        @ws = @WebSocketService.createWebSocket({
          callbacks: {
            onopen : @onopen,
            onclose : @onclose,
            onmessage : @onmessage
          }
        })
        configureFinished.promise.then(() => 
          initiateFinished.resolve()
        )
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  configure: (session, configs, configureFinished) ->
    @TestService.configure(@specId, session, configs)
    .then(
      (data) =>
        @simulatedConfigs = data.configs

        @$log.debug "-------------------"
        @$log.debug @simulatedConfigs

        if @isOwner
          msg = {
            command: @Constants.WEB_SOCKET_COMMAND.NOTIFY
            sessionId: @session
            simulatedConfigs: @simulatedConfigs
          }
          @ws.send(angular.toJson(msg))

        if @currentTest.preliminary?
          @startAutomatically = false
          @initiatePreliminary(@session, configureFinished)
        else
          configureFinished.resolve()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  initiatePreliminary: (session, configureFinished) ->
    @TestService.initiatePreliminary(session)
    .then(
      (data) =>
        configureFinished.resolve()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  backDisabled: () ->
    false

  back: () ->
    @$state.go 'app.systems.detail.conformance.detail', {actor_id: @actorId, specId: @specId, id: @systemId}

  reinitialise: () =>
    @$state.go @$state.current, {}, {reload: true}

  start: (session) ->
    @updateTestCaseStatus(@currentTest.id, @Constants.TEST_CASE_STATUS.PROCESSING)
    @started = true
    @firstTestStarted = true
    @startAutomatically = true
    @ReportService.createTestReport(session, @systemId, @actorId, @currentTest.id).then(() =>
      @TestService.start(session)
      .then(
        (data) =>
          @$log.debug data
        ,
        (error) =>
          @ErrorService.showErrorMessage(error, true).result.then () =>
            @$state.go @$state.current, {}, {reload: true}
      )
    )

  stop: (session) =>
    @started = false
    @TestService.stop(session)
    .then(
      (data) =>
        if (@ws?)
          @ws.close()
        @session = null
        @ws = null
        @testCaseFinished()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  restart: (session) ->
    @TestService.restart(session)
    .then(
      (data) =>
        @$log.debug data
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  createActorConfigurations: (configs) ->
    if @endpoints?.length > 0
      #TODO cover all endpoints
      endpoint = @endpoints[0]

      configurations = {
        actor: endpoint.actor.actorId,
        endpoint: endpoint.name,
        config: []
      }

      for config in endpoint.parameters
        configurations.config.push({
          name: config.name,
          value: config.value
        })

      @$log.debug angular.toJson(configurations)

      configurations


  onopen: (msg) =>
    @$log.debug "WebSocket created."
    testType = -1
    if @isInteroperabilityTesting
      testType = @Constants.TEST_CASE_TYPE.INTEROPERABILITY
    else
      testType = @Constants.TEST_CASE_TYPE.CONFORMANCE
    #register client
    msg = {
      command: @Constants.WEB_SOCKET_COMMAND.REGISTER
      sessionId: @session,
      actorId:   @actor
      type: testType
    }
    if !@isOwner
      msg.configurations = @createActorConfigurations(@configurations)

    @ws.send(angular.toJson(msg))

    @keepAlive = @$interval(() => 
      if (@ws?)
        msg = {
          command: @Constants.WEB_SOCKET_COMMAND.PING
        }      
        @ws.send(angular.toJson(msg))
    , 5000)

    @socketOpen.resolve()

  onclose: (msg) =>
    @$log.debug "WebSocket closed."
    if (@keepAlive?)
      @$interval.cancel(@keepAlive)

  onmessage: (msg) =>
    if (!@messagesToProcess?)
      @messagesToProcess = []
    @messagesToProcess.push(msg)

  processNextMessage: () =>
    if (@messagesToProcess? && @messagesToProcess.length > 0)
      msg = @messagesToProcess.shift()
      @processMessage(msg)

  processMessage: (msg) =>
    response = angular.fromJson(msg.data)
    stepId = response.stepId
    if response.interactions? #interactWithUsers
        @interact(response.interactions, stepId)
    else if response.notify?
      if response.notify.simulatedConfigs?
        @simulatedConfigs = response.notify.simulatedConfigs
    else if response.configuration? #actorConfigurations for interoperability testing
      if @isOwner
        @actorConfigurations[response.configuration.actor] = response.configuration
        list = _.values(@actorConfigurations)
        if !_.contains(list, null)
          @$scope.$broadcast 'wizard-directive:next' #proceed to 3rd step after all configurations received
          @configure(@session, list)
    else  #updateStatus
      if stepId == @Constants.END_OF_TEST_STEP
        @$log.debug "END OF THE TEST"
        @started = false
        @testCaseFinished(response.report.result)
      else
        status = response.status
        report = response.report
        step   = @findNodeWithStepId @$scope.stepsOfTests[@currentTest.id], stepId
        if report?
          report.tcInstanceId = response.tcInstanceId
        @updateStatus(step, stepId, status, report)
        if !@started && report?.result == "FAILURE"
          error = {
            statusText: 'Preliminary step error',
            data: {
              error_description: ''
            }
          }
          if report?.reports?.assertionReports? &&
          report.reports.assertionReports.length > 0 &&
          report.reports.assertionReports[0].value?.description?
              error.data.error_description = report.reports.assertionReports[0].value.description

          @ErrorService.showErrorMessage(error)

  interact: (interactions, stepId) =>
    sessionForModal = @session

    modalOptions =
      templateUrl: 'assets/views/tests/provide-input-modal.html'
      controller: 'ProvideInputModalController as controller'
      keyboard: false,
      backdrop: 'static'      
      resolve: 
        interactions: () => interactions
        session: () => sessionForModal
        interactionStepId: () => stepId

    modalInstance = @$modal.open(modalOptions)
    modalInstance.result.then((result) => 
      if (!result.success)
        @ErrorService.showErrorMessage(result.error, true).result.then () =>
        @$state.go @$state.current, {}, {reload: true}
    )

  updateStatus: (step, stepId, status, report) =>

    endsWith = (str, suffix) ->
      str.indexOf suffix, (str.length - suffix.length) != -1

    if step?
      if step.id != stepId
        current = step

        while current? &&
        current.id != stepId
          console.debug 'Looking for ['+stepId+'] in its parent step ['+current.id+']'
          if current.type == 'loop'
            if !current.sequences?
              current.sequences = []

            setIds = (steps, str, replacement) ->
              _.forEach steps, (step) =>
                step.id = step.id.replace str, replacement

                if step.type == 'loop'
                  setIds step.steps, str, replacement
                else if step.type == 'flow'
                  setIds step.threads, str, replacement
                else if step.type == 'decision'
                  setIds step.then, str, replacement
                  setIds step.else, str, replacement

                return

            clearStatusesAndReports = (steps) ->
              _.forEach steps, (step) =>
                delete step.status
                delete step.report

                if step.type == 'loop'
                  clearStatusesAndReports step.steps
                else if step.type == 'decision'
                  clearStatusesAndReports step.then
                  clearStatusesAndReports step.else
                else if step.type == 'flow'
                  clearStatusesAndReports step.threads

            copySteps = _.cloneDeep current.steps
            clearStatusesAndReports copySteps

            index = Number(stepId.substring ((stepId.indexOf '[', current.id.length)+1), (stepId.indexOf ']', current.id.length))

            oldId = (current.id + '[1]')
            newId = (current.id + '[' + index + ']')

            setIds copySteps, oldId, newId

            if !current.sequences[index - 1]?
              console.debug 'Creating a new step sequence with id ['+newId+']'
              current.sequences[index - 1] =
                id: newId
                type: current.type
                steps: copySteps

            current = @findNodeWithStepId current.sequences[index - 1], stepId
          else
            break
      else
        current = step

      if current? &&
      current.status != @Constants.TEST_STATUS.COMPLETED &&
      current.status != @Constants.TEST_STATUS.ERROR &&
      current.status != @Constants.TEST_STATUS.SKIPPED
        current.status = status
        current.report = report

  isParent: (id, parentId) ->
    periodIndex = id.indexOf '.', parentId.length
    paranthesesIndex = id.indexOf '[', parentId.length
    (id.indexOf parentId) == 0 && (periodIndex == parentId.length || paranthesesIndex == parentId.length)

  findNodeWithStepId: (steps, id) ->
    filter = (step) =>
      console.debug 'Looking for ['+id+'] in step ['+step.id+']'
      if step.id == id
        console.debug 'Found step with id ['+id+']'
        return step
      else if @isParent id, step.id
        parent = step
        s = null

        if parent.type == 'loop'
          console.debug 'Found a parent loop step for id ['+id+'] with id ['+parent.id+']'
          if parent.sequences?
            for sequence in parent.sequences
              if sequence?
                if sequence.id == id
                  console.debug 'Found matching loop sequence for id ['+id+']'
                  s = sequence
                else if @isParent id, sequence.id
                  console.debug 'Found matching parent loop sequence for id ['+id+'] with id ['+sequence.id+']. Looking at its steps.'
                  s = @findNodeWithStepId sequence.steps, id
                  if s?
                    break
        else if parent.type == 'decision'
          console.debug 'Found a parent decision step for id ['+id+'] with id ['+parent.id+']'
          s = @findNodeWithStepId parent.then, id

          if !s?
            s = @findNodeWithStepId parent.else, id
        else if parent.type == 'flow'
          console.debug 'Found a parent flow step for id ['+id+'] with id ['+parent.id+']. Looking at its threads.'
          for thread in parent.threads
            s = @findNodeWithStepId thread, id
            if s?
              break

        if s?
          return s
        else
          return parent
      else
        return null

    for step in steps
      if step?
        parentOrCurrentNode = filter step
        if parentOrCurrentNode?
          return parentOrCurrentNode

    return null

controllers.controller('TestExecutionControllerV2', TestExecutionControllerV2)
