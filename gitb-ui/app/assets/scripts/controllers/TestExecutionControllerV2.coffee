class TestExecutionControllerV2
  @$inject = ['$window','$log', '$scope', '$location', '$uibModal', '$state', '$stateParams', 'Constants', 'TestService', 'SystemService', 'ConformanceService', 'WebSocketService', 'ReportService', 'ErrorService', 'DataService', '$q', '$timeout', '$interval', 'OrganizationService', 'PopupService']
  constructor: (@$window, @$log, @$scope, @$location, @$uibModal, @$state, @$stateParams, @Constants, @TestService, @SystemService, @ConformanceService, @WebSocketService, @ReportService, @ErrorService, @DataService, @$q, @$timeout, @$interval, @OrganizationService, @PopupService) ->
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
              @ErrorService.showErrorMessage(error).finally(angular.noop).then(angular.noop, angular.noop) 
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
              @ErrorService.showErrorMessage(error).finally(angular.noop).then(angular.noop, angular.noop)
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
    @$scope.interactions  = []
    @selectedInteroperabilitySession = null
    @isOwner = false
    @wizardStep = 0
    @intervalSet = false
    @progressIcons = {}
    @testCaseStatus = {}
    @$scope.stepsOfTests = {}
    @$scope.actorInfoOfTests = {}
    @logMessages = {}
    @$scope.$on '$destroy', () =>
      @leavingTestExecutionPage()
    @stopped = false
    @currentTestIndex = 0
    @currentTest = @testsToExecute[@currentTestIndex]
    @visibleTest = @currentTest
    for test in @testsToExecute
      @updateTestCaseStatus(test.id, @Constants.TEST_CASE_STATUS.PENDING)
      @$scope.stepsOfTests[test.id] = {}
      @$scope.actorInfoOfTests[test.id] = {}

    @organisationConfigurationChecked = @$q.defer()
    @systemConfigurationChecked = @$q.defer()
    @configurationChecked = @$q.defer()
    @testCaseLoaded = @$q.defer()
    @$q.all([@organisationConfigurationChecked.promise, @systemConfigurationChecked.promise, @configurationChecked.promise, @testCaseLoaded.promise]).then(() =>
      @nextStep()
    )
    @checkConfigurations(@actorId, @systemId)
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
    !(configStr1 == configStr2)

  nextStep: (stepToConsider) =>
    if (!@stopped)
      if (!stepToConsider?)
        stepToConsider = @wizardStep
      if (stepToConsider == 0)
        # Before starting
        @configurationValid = @isConfigurationValid()
        @systemConfigurationValid = @isMemberConfigurationValid(@systemProperties)
        @organisationConfigurationValid = @isMemberConfigurationValid(@organisationProperties)
        if (!@configurationValid || !@systemConfigurationValid || !@organisationConfigurationValid)
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
    # Make sure steps still marked as pending or in progress are set as skipped.
    @setPendingStepsToSkipped()
    if (@currentTestIndex + 1 < @testsToExecute.length)
      @$timeout(() => 
        @nextStep()
      , 1000)
    else
      @nextStep()

  getOrganisation : () =>
    organisation = @DataService.vendor
    if @DataService.isCommunityAdmin || @DataService.isSystemAdmin
      organisation = JSON.parse(@$window.localStorage['organization'])
    organisation

  checkConfigurations : (actorId, systemId) ->
    @OrganizationService.getOrganisationParameterValues(@getOrganisation().id, false)
    .then (data) =>
      @organisationProperties = data
      @organisationConfigurationChecked.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error, true)
      .then(() =>
        @$state.go @$state.current, {}, {reload: true})
      .catch(angular.noop)

    @SystemService.getSystemParameterValues(systemId, false)
    .then (data) =>
      @systemProperties = data
      @systemConfigurationChecked.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error, true)
      .then(() =>
        @$state.go @$state.current, {}, {reload: true})
      .catch(angular.noop)

    @ConformanceService.checkConfigurations(actorId, systemId)
    .then (data) =>
      @endpointRepresentations = data
      @configurationChecked.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error, true)
      .then(() =>
        @$state.go @$state.current, {}, {reload: true})
      .catch(angular.noop)

  isMemberConfigurationValid: (properties) ->
    valid = true
    if properties?
      for property in properties
        if property.use == 'R' && !property.configured
          return false
    valid

  isConfigurationValid: () ->
    valid = true
    if @endpointRepresentations?
      for endpoint in @endpointRepresentations
        for parameter in endpoint.parameters
          if !parameter.configured && parameter.use == "R"
            return false
    valid

  getTestCaseDefinition: (testCaseToLookup) ->
    @TestService.getTestCaseDefinition(testCaseToLookup)
    .then(
      (data) =>
        testcase = data
        if (testcase.preliminary?)
          @currentTest["preliminary"] = testcase.preliminary
        
        @TestService.getActorDefinitions(@specId).then((data) =>
          tempActors = testcase.actors.actor
          for domainActorData in data
            if (domainActorData.id == @actorId)
              @actor = domainActorData.actorId
            for testCaseActorData in tempActors
              if (testCaseActorData.id == domainActorData.actorId)
                if !(testCaseActorData.name?)
                  testCaseActorData.name = domainActorData.name
                if !(testCaseActorData.displayOrder?) && domainActorData.displayOrder?
                  testCaseActorData.displayOrder = domainActorData.displayOrder
                break
          tempActors = tempActors.sort((a, b) =>
            if !a.displayOrder? && !b.displayOrder?
              0
            else if a.displayOrder? && !b.displayOrder?
              -1
            else if !a.displayOrder? && b.displayOrder?
              1
            else
              Number(a.displayOrder) - Number(b.displayOrder)
          )
          @$scope.actorInfoOfTests[testCaseToLookup] = tempActors

          @$scope.stepsOfTests[testCaseToLookup] = testcase.steps
          @$scope.$broadcast('sequence:testLoaded', testCaseToLookup)
          @testCaseLoaded.resolve()
        ,
        (error) =>
          @ErrorService.showErrorMessage(error, true)
          .then(() =>
            @$state.go @$state.current, {}, {reload: true})
          .catch(angular.noop)
        )
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true)
        .then(() =>
          @$state.go @$state.current, {}, {reload: true})
        .catch(angular.noop)
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
        configureFinished = @$q.defer()
        @configure(@session, configureFinished)

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
        @ErrorService.showErrorMessage(error).finally(angular.noop).then(angular.noop, angular.noop) 
    )

  configure: (session, configureFinished) ->
    @TestService.configure(@specId, session, @systemId, @actorId)
    .then(
      (data) =>
        @simulatedConfigs = data.configs

        if @currentTest.preliminary?
          @startAutomatically = false
          @initiatePreliminary(@session, configureFinished)
        else
          configureFinished.resolve()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true)
        .then(() =>
          @$state.go @$state.current, {}, {reload: true})
        .catch(angular.noop)
    )

  initiatePreliminary: (session, configureFinished) ->
    @TestService.initiatePreliminary(session)
    .then(
      (data) =>
        configureFinished.resolve()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true)
        .then(() =>
          @$state.go @$state.current, {}, {reload: true})
        .catch(angular.noop)
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
          @ErrorService.showErrorMessage(error, true)
          .then(() =>
            @$state.go @$state.current, {}, {reload: true})
          .catch(angular.noop)
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
        @ErrorService.showErrorMessage(error, true)
        .then(() =>
          @$state.go @$state.current, {}, {reload: true})
        .catch(angular.noop)
    )

  restart: (session) ->
    @TestService.restart(session)
    .then(
      (data) =>
        @$log.debug data
      ,
      (error) =>
        @ErrorService.showErrorMessage(error, true)
        .then(() =>
          @$state.go @$state.current, {}, {reload: true})
        .catch(angular.noop)
    )

  onopen: (msg) =>
    @wsOpen = true
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

    @ws.send(angular.toJson(msg))

    @keepAlive = @$interval(() => 
      if (@ws? && @wsOpen)
        msg = {
          command: @Constants.WEB_SOCKET_COMMAND.PING
        }      
        @ws.send(angular.toJson(msg))
    , 5000)

    @socketOpen.resolve()

  onclose: (msg) =>
    @$log.debug "WebSocket closed."
    @wsOpen = false
    if (@keepAlive?)
      @$interval.cancel(@keepAlive)

  onmessage: (msg) =>
    response = angular.fromJson(msg.data)
    stepId = response.stepId
    if stepId == @Constants.LOG_EVENT_TEST_STEP    
      # Process log messages immediately
      if @currentTest? && @currentTest.id?
        if !@logMessages[@currentTest.id]?
          @logMessages[@currentTest.id] = []
      @logMessages[@currentTest.id].push(response.report.context.value)
    else
      if (!@messagesToProcess?)
        @messagesToProcess = []
      @messagesToProcess.push(msg)

  processNextMessage: () =>
    if (@messagesToProcess? && @messagesToProcess.length > 0)
      msg = @messagesToProcess.shift()
      @processMessage(msg)

  setPendingStepsToSkipped: () =>
    for step in @$scope.stepsOfTests[@currentTest.id]
      if step.status == @Constants.TEST_STATUS.PROCESSING || step.status == @Constants.TEST_STATUS.WAITING
        step.status = @Constants.TEST_STATUS.SKIPPED

  processMessage: (msg) =>
    response = angular.fromJson(msg.data)
    stepId = response.stepId
    if response.interactions? #interactWithUsers
        @interact(response.interactions, stepId)
    else if response.notify?
      if response.notify.simulatedConfigs?
        @simulatedConfigs = response.notify.simulatedConfigs
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
        if stepId+'' == '0' && report?.result == "FAILURE"
          # stepId is 0 for the preliminary step
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

          @ErrorService.showErrorMessage(error).finally(angular.noop).then(angular.noop, angular.noop)

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

    modalInstance = @$uibModal.open(modalOptions)
    modalInstance.result
      .finally(angular.noop)
      .then((result) => 
        if (!result.success)
          @ErrorService.showErrorMessage(result.error, true).then () =>
          @$state.go @$state.current, {}, {reload: true}
      , angular.noop)

  updateStatus: (step, stepId, status, report) =>
    if step?
      if step.id != stepId
        current = step

        while current? &&
        current.id != stepId
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
              current.sequences[index - 1] =
                id: newId
                type: current.type
                steps: copySteps

            current = @findNodeWithStepId current.sequences[index - 1], stepId
          else
            break
      else
        current = step

      if current? && current.status != status
        if (status == @Constants.TEST_STATUS.COMPLETED) ||
        (status == @Constants.TEST_STATUS.ERROR) ||
        (status == @Constants.TEST_STATUS.WARNING) ||
        (status == @Constants.TEST_STATUS.SKIPPED && (current.status != @Constants.TEST_STATUS.COMPLETED && current.status != @Constants.TEST_STATUS.ERROR && current.status != @Constants.TEST_STATUS.WARNING)) ||
        ((status == @Constants.TEST_STATUS.PROCESSING || status == @Constants.TEST_STATUS.WAITING) && (@started && current.status != @Constants.TEST_STATUS.COMPLETED && current.status != @Constants.TEST_STATUS.ERROR && current.status != @Constants.TEST_STATUS.SKIPPED && current.status != @Constants.TEST_STATUS.WARNING))
          current.status = status
          current.report = report
          # If skipped, marked all children as skipped.
          if status == @Constants.TEST_STATUS.SKIPPED
            @setChildrenAsSkipped(current, stepId, stepId)

  escapeRegExp: (text) ->
    text.replace(/\./g, "\\.").replace(/\[/g, "\\[").replace(/\]/g, "\\]")

  setChildrenSequenceAsSkipped: (sequence, parentStepId) ->
    if sequence?
      for childStep in sequence
        if Array.isArray(childStep)
          for childStepItem in childStep
            @setChildrenAsSkipped(childStepItem, childStepItem.id, parentStepId)
        else
          @setChildrenAsSkipped(childStep, childStep.id, parentStepId)

  setChildrenAsSkipped: (step, idToCheck, parentStepId) ->
    regex = new RegExp(@escapeRegExp(parentStepId)+"(\\[.+\\])?\\.?", "g");
    console.log("Checking ["+idToCheck+"] vs ["+parentStepId+"]")
    if step? && idToCheck? && (idToCheck == parentStepId || idToCheck.match(regex) != null)
      if step.type == 'loop'
        @setChildrenSequenceAsSkipped(step.steps, idToCheck)
      else if step.type == 'decision'
        @setChildrenSequenceAsSkipped(step.then, idToCheck)
        @setChildrenSequenceAsSkipped(step.else, idToCheck)
      else if step.type == 'flow'
        @setChildrenSequenceAsSkipped(step.threads, idToCheck)
      step.status = @Constants.TEST_STATUS.SKIPPED

  isParent: (id, parentId) ->
    periodIndex = id.indexOf '.', parentId.length
    paranthesesIndex = id.indexOf '[', parentId.length
    (id.indexOf parentId) == 0 && (periodIndex == parentId.length || paranthesesIndex == parentId.length)

  findNodeWithStepId: (steps, id) ->
    filter = (step) =>
      if step.id == id
        return step
      else if @isParent id, step.id
        parent = step
        s = null

        if parent.type == 'loop'
          if parent.sequences?
            for sequence in parent.sequences
              if sequence?
                if sequence.id == id
                  s = sequence
                else if @isParent id, sequence.id
                  s = @findNodeWithStepId sequence.steps, id
                  if s?
                    break
        else if parent.type == 'decision'
          s = @findNodeWithStepId parent.then, id

          if !s?
            s = @findNodeWithStepId parent.else, id
        else if parent.type == 'flow'
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

    if steps?
      for step in steps
        if step?
          parentOrCurrentNode = filter step
          if parentOrCurrentNode?
            return parentOrCurrentNode

    return null

  showViewLog: () =>
    @visibleTest?

  viewLog: () =>
    if @logMessages[@visibleTest.id]?
      value = @logMessages[@visibleTest.id].join('')
    else
      value = ''
    modalOptions =
      templateUrl: 'assets/views/components/editor-modal.html'
      controller: 'EditorModalController as editorModalCtrl'
      resolve:
        name: () => 'Test session log'
        editorOptions: () =>
          value: value
          readOnly: true
          lineNumbers: true
          smartIndent: false
          electricChars: false
          mode: 'text/plain' 
          download: {
            fileName: 'log.txt'
            mimeType: 'text/plain'
          }
        indicators: () => null
        lineNumber: () => null

      size: 'lg'

    @$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)

  toOrganisationProperties: () =>
    if @DataService.isVendorUser || @DataService.isVendorAdmin
      @$state.go 'app.settings.organisation', {viewProperties: true}
    else
      organisation = @getOrganisation()
      if @DataService.vendor.id == organisation.id
        @$state.go 'app.settings.organisation', {viewProperties: true}
      else
        @OrganizationService.getOrganizationBySystemId(@systemId)
        .then (org) =>
          @$state.go 'app.admin.users.communities.detail.organizations.detail.list', {org_id: org.id, community_id: org.community, viewProperties: true}
        .catch (error) =>
          @ErrorService.showErrorMessage(error)

  toSystemProperties: () =>
    if @DataService.isVendorUser
      @$state.go 'app.systems.detail.info', {id: @systemId, viewProperties: true}
    else
      @$state.go 'app.systems.list', {id: @systemId, viewProperties: true}
  
  toConfigurationProperties: () =>
    @$state.go 'app.systems.detail.conformance.detail', {actor_id: @actorId, specId: @specId, id: @systemId, editEndpoints: true}

  leavingTestExecutionPage: () =>
    if @ws? and @session?
      @ws.close()
    if @firstTestStarted && !@stopped
      pendingTests = _.filter(@testsToExecute, (test) =>
        @testCaseStatus[test.id] == @Constants.TEST_CASE_STATUS.READY || @testCaseStatus[test.id] == @Constants.TEST_CASE_STATUS.PENDING
      )
      pendingTestIds = _.map(pendingTests, (test) =>
        test.id
      )
      if pendingTestIds.length > 0
        @TestService.startHeadlessTestSessions(pendingTestIds, @specId, @systemId, @actorId)
        @PopupService.success('Continuing test sessions in background. Check <b>Test Sessions</b> for progress.')
      else
        if @testCaseStatus[@currentTest.id] == @Constants.TEST_CASE_STATUS.PROCESSING
          @PopupService.success('Continuing test session in background. Check <b>Test Sessions</b> for progress.')

controllers.controller('TestExecutionControllerV2', TestExecutionControllerV2)
