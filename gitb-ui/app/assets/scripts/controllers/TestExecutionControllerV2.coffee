class TestExecutionControllerV2
  @$inject = ['$log', '$scope', '$location', '$modal', '$state', '$stateParams', 'Constants', 'TestService', 'SystemService', 'ConformanceService', 'WebSocketService', 'ReportService', 'ErrorService']
  constructor: (@$log, @$scope, @$location, @$modal, @$state, @$stateParams, @Constants, @TestService, @SystemService, @ConformanceService, @WebSocketService, @ReportService, @ErrorService) ->
    @$log.debug "Constructing TestExecutionController..."

    @testId   = @$stateParams['test_id']
    @systemId = @$stateParams['systemId']
    @actorId  = @$stateParams['actorId']
    @specId   = @$stateParams['specId']
    @started	= false #test started or not
    @reload	  = false
    @actorConfigurations = {}
    @$scope.interactions  = []
    @selectedInteroperabilitySession = null
    @isOwner = false
    @ready	 = false

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

    @steps = [
      {
        id: 1
        title: 'System Configurations'
      }
      {
        id: 2
        title: 'Initiate Preliminary'
      }
      {
        id: 3
        title: 'Execute Test'
      }
    ]

    @getEndpointConfigurations(@actorId)

    @getTestCaseDefinition(@testId)
    @getActorDefinition(@actorId)

  nextStep: () =>
    @$scope.$broadcast 'wizard-directive:next'

  onWizardNext: (step) =>
    if step.id == 1
      if !@isInteroperabilityTesting
        @initiate(@testId)
      true
    else
      true

  onWizardFinish: () =>
    @$log.debug "xxx"

  onWizardBefore: (step) =>
    if step.id == 1
      !@isSystemConfigurationsValid(@endpointRepresentations)
    else if step.id == 2
      true
    else
      true

  onFileSelect: (request, files) =>
    request.file = _.head files
    if request.file?
      reader = new FileReader()
      reader.readAsDataURL request.file
      reader.onload = (event) =>
        request.data = event.target.result

  checkReady: ()->
    @ready = @endpointsLoaded && @configurationsLoaded && @actorLoaded
    if @ready
      @$scope.$broadcast 'wizard-directive:start'
    @ready

  updateConfigurations: ()->
    @$state.go 'app.systems.detail.conformance.detail', {id: @systemId, actor_id: @actorId, specId:@specId}

  getEndpointConfigurations :(actorId)->
    @ConformanceService.getEndpointsForActor actorId
        .then (endpoints) =>
          @endpoints = endpoints
          @endpointsLoaded = true
          @checkReady()
        .then () =>
          if @endpoints?.length > 0
            endpointIds = _.map @endpoints, (endpoint) -> endpoint.id
            @SystemService.getConfigurationsWithEndpointIds(endpointIds, @systemId)
            .then (configurations) =>
              @configurations = configurations
              @constructEndpointRepresentations()
              @configurationsLoaded = true
              @checkReady()

              @$log.debug @endpointRepresentations

              for configuration in @configurations
                endpoint = @getEndpointForId(configuration.endpoint)
                for parameter in endpoint.parameters
                  if parameter.id == configuration.parameter
                    parameter.value = configuration.value
          else
            @configurationsLoaded = true
            @checkReady()

        .catch (error) =>
          @ErrorService.showErrorMessage(error).result.then () =>
            @$state.go @$state.current, {}, {reload: true}

  constructEndpointRepresentations: () =>
    @endpointRepresentations = _.map @endpoints, (endpoint) =>
        name: endpoint.name
        desc: endpoint.desc
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

  isConfigurationDataURL: (configuration) ->
    @Constants.DATA_URL_REGEX.test(configuration)

  getTestCaseDefinition: (testCase) ->
    @TestService.getTestCaseDefinition(testCase)
    .then(
      (data) =>
        @testcase = data
        #@$log.debug angular.toJson(data)
        @$scope.steps = @testcase.steps
        @$scope.actorInfo = @testcase.actors.actor
        @testCaseLoaded = true

        @isInteroperabilityTesting = @testcase.metadata.type == @Constants.TEST_CASE_TYPE.INTEROPERABILITY
        if @isInteroperabilityTesting
          @steps.splice(1, 0, {id:2, title:"Participants" })
          @steps[2].id = 3
          @steps[3].id = 4

          @getInteroperabilitySessions()

        for testCaseActor in @$scope.actorInfo
          if testCaseActor.role == @Constants.TEST_ROLE.SUT
            @actorConfigurations[testCaseActor.id] = null
      ,
      (error) =>
        @ErrorService.showErrorMessage(error).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  getActorDefinition: (actorId) ->
    @TestService.getActorDefinition(actorId)
    .then(
      (data) =>
        @actor = data.actorId
        @actorLoaded = true
        @checkReady()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error).result
    )

  generateStepIdMap: (steps) =>
    _.forEach steps, (step) =>
      @stepsMap[step.id] = step

      if step.type == 'decision'
        @generateStepsMap step.then
        @generateStepsMap step.else
      else if step.type == 'loop'
        @generateStepsMap step.steps

  onSessionSelect: (session) =>
    @selectedSession = session.session
    @availableActors = []
    for testCaseActor in @$scope.actorInfo
      found = _.find session.actors, (unavailableActor) => unavailableActor == testCaseActor.id
      if !found? #actor not found, so it is available
        @availableActors.push({actor: testCaseActor.id})

  onActorSelect: (actor) =>
    @actor = actor.actor

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
    @initiate(@testId)

  initiate: (testCase) ->
    @TestService.initiate(testCase)
    .then(
      (data) =>
        @session = data
        @actorConfigurations[@actor] = @createActorConfigurations(@configurations)
        list = _.values(@actorConfigurations)

        #if actor configurations list contains null values, that means other actors of interoperability
        #testing did not send their configurations yet. WON'T happen in Conformance Testing
        if !_.contains(list, null)
          @configure(@session, list)

        #create a WebSocket when a new session is initiated
        @ws = @WebSocketService.createWebSocket({
          callbacks: {
            onopen : @onopen,
            onclose : @onclose,
            onmessage : @onmessage
          }
        })
      ,
      (error) =>
        @ErrorService.showErrorMessage(error).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  configure: (session, configs) ->
    @TestService.configure(session, configs)
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

        if @testcase.preliminary?
          @initiatePreliminary(@session)
          @showNextForPreliminaryStep = true
        else
          if !(@simulatedConfigs && @simulatedConfigs.length > 0)
            @nextStep()
          else
            @showNextForPreliminaryStep = true
      ,
      (error) =>
        @ErrorService.showErrorMessage(error).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  provideInput: () ->
    @closeInteractionModal()
    inputs = []
    for interaction in @$scope.interactions
      if interaction.type == "request"
        inputs.push({
          id: interaction.id,
          name:  interaction.name
          value: interaction.data
          type:  interaction.variableType,
          embeddingMethod: interaction.contentType
        })

    @TestService.provideInput(@session, @$scope.interactionStepId, inputs)
    .then(
      (data) =>
        @$log.debug data
      ,
      (error) =>
        @ErrorService.showErrorMessage(error).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )
    @$scope.interactionStepId = null
    @$scope.interactions = []

  initiatePreliminary: (session) ->
    @TestService.initiatePreliminary(session)
    .then(
      (data) =>
        @$log.debug data
      ,
      (error) =>
        @ErrorService.showErrorMessage(error).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  start: (session) ->
    if @reload
      @$log.debug 'reloading...'
      @$state.go @$state.current, {}, {reload: true}
    else
      @started = true
      @ReportService.createTestReport(session, @systemId, @actorId, @testId)
      @TestService.start(session)
      .then(
        (data) =>
          @$log.debug data
        ,
        (error) =>
          @ErrorService.showErrorMessage(error).result.then () =>
            @$state.go @$state.current, {}, {reload: true}
      )

  stop: (session) ->
    @reload = true
    @started = false
    @TestService.stop(session)
    .then(
      (data) =>
        @ws.close()
        @session = null
        @ws = null
      ,
      (error) =>
        @ErrorService.showErrorMessage(error).result.then () =>
          @$state.go @$state.current, {}, {reload: true}
    )

  restart: (session) ->
    @TestService.restart(session)
    .then(
      (data) =>
        @$log.debug data
      ,
      (error) =>
        @ErrorService.showErrorMessage(error).result.then () =>
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

  onclose: (msg) =>
    @$log.debug "WebSocket closed."

  onmessage: (msg) =>
    response = angular.fromJson(msg.data)
    #@$log.debug msg.data

    stepId = response.stepId

    if response.interactions? #interactWithUsers
        @interact(response.interactions, stepId)
    else if response.notify?
      if response.notify.simulatedConfigs?
        @$scope.$apply () =>
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
        @$scope.$apply () =>
          @started = false
          @reload  = true
          return ;
      else
        status = response.status
        report = response.report
        step   = @findNodeWithStepId @$scope.steps, stepId
        if report?
          report.tcInstanceId = response.tcInstanceId
        @updateStatus(step, stepId, status, report)

  interact: (interactions, stepId) ->
    @$scope.interactionStepId = stepId
    for interaction in interactions

      @$scope.$apply () =>
        @$scope.interactions.push(interaction)

    $('#provideInputModal').modal({
        keyboard: false,
        backdrop: 'static'
    }, 'toggle');

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
        @$scope.$apply () =>
          console.debug 'Setting the status for ', current, status
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

  closeInteractionModal: () ->
    $('#provideInputModal').modal('toggle')
    return ;

  interactionNeedsInput: () ->
    for interaction in @$scope.interactions
      if interaction.type == "request"
        return true
    return false


controllers.controller('TestExecutionControllerV2', TestExecutionControllerV2)
