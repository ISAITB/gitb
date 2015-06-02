class TestExecutionController

    @$inject = ['$log', '$scope', '$modal', '$state', '$stateParams', 'Constants', 'TestService', 'SystemService', 'ConformanceService', 'WebSocketService', 'ReportService', 'ErrorService']
    constructor: (@$log, @$scope, @$modal, @$state, @$stateParams, @Constants, @TestService, @SystemService, @ConformanceService, @WebSocketService, @ReportService, @ErrorService) ->
        @$log.debug "Constructing TestExecutionController..."

        @testId   = @$stateParams['test_id']
        @systemId = @$stateParams['systemId']
        @actorId  = @$stateParams['actorId']
        @systemConfigurations = []
        @flat     = {}
        @iteration= {}
        @loops	 = []

        @ws = WebSocketService.createWebSocket({
            callbacks: {
                onopen : @onopen,
                onclose : @onclose,
                onmessage : @onmessage
            }
        })

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

    nextStep: () =>
        @$scope.$broadcast 'wizard-directive:next'

    onWizardNext: (step) =>
        if step.id == 1
            @initiate(@testId)
            true
        else if step.id == 2
            true
        else
            true

    onWizardFinish: () =>
        @$log.debug "xxx"

    updateConfigurations: ()->
        @$state.go 'app.systems.detail.configurations', {id: @systemId}

    getEndpointConfigurations :(actorId)->
        @ConformanceService.getEndpointsForActor actorId
                .then (endpoints) =>
                    @endpoints = endpoints
                .then () =>
                    endpointIds = _.map @endpoints, (endpoint) -> endpoint.id
                    @SystemService.getConfigurationsWithEndpointIds(endpointIds, @systemId)
                .then (configurations) =>
                    @configurations = configurations
                    @constructEndpointRepresentations()

                    @$log.debug @endpointRepresentations

                    for configuration in @configurations
                        endpoint = @getEndpointForId(configuration.endpoint)
                        for parameter in endpoint.parameters
                            if parameter.id == configuration.parameter
                                parameter.value = configuration.value
                .catch (error) =>
                    @$log.error 'An error occurred', error

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
                    if !parameter.configured
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

    getTestCaseDefinition: (testCase) ->
        @TestService.getTestCaseDefinition(testCase)
        .then(
            (data) =>
                @testcase = data
                @$log.debug angular.toJson(data)
                @$scope.steps = @testcase.steps
                @flatten(@$scope.steps)
            ,
            (error) =>
                @ErrorService.showErrorMessage(error)
        )

    initiate: (testCase) ->
        @TestService.initiate(testCase)
        .then(
            (data) =>
                @session = data
                @configure(@session, @createActorConfigurations(@configurations))

                #register client
                msg = {
                    sessionId: @session,
                    actorId: @actorId
                }
                @$log.debug angular.toJson(msg)
                @ws.send(angular.toJson(msg))
            ,
            (error) =>
                @ErrorService.showErrorMessage(error)
        )

    configure: (session, configs) ->
        @TestService.configure(session, configs)
        .then(
            (data) =>
                @simulatedConfigs = data.configs

                if @testcase.preliminary?
                    @initiatePreliminary(@session)
            ,
            (error) =>
                @ErrorService.showErrorMessage(error)
        )

    provideInput: () ->
        $('#provideInputModal').modal('toggle');
        inputs = []
        for request in @$scope.requests
            inputs.push({
                id: request.id,
                item: [
                    {
                        name: request.name
                        value: request.data
                    }
                ]
            })

        @TestService.provideInput(@session, @$scope.interactionStepId, inputs)
        .then(
            (data) =>
                @$log.debug data
            ,
            (error) =>
                @ErrorService.showErrorMessage(error)
        )

    initiatePreliminary: (session) ->
        @TestService.initiatePreliminary(session)
        .then(
            (data) =>
                @$log.debug data
            ,
            (error) =>
                @ErrorService.showErrorMessage(error)
        )

    start: (session) ->
        @ReportService.createTestReport(session, @systemId, @actorId, @testId)
        @TestService.start(session)
        .then(
            (data) =>
                @$log.debug data
            ,
            (error) =>
                @ErrorService.showErrorMessage(error)
        )

    stop: (session) ->
        @TestService.stop(session)
        .then(
            (data) =>
                @$log.debug data
            ,
            (error) =>
                @ErrorService.showErrorMessage(error)
        )

    restart: (session) ->
        @TestService.restart(session)
        .then(
            (data) =>
                @$log.debug data
            ,
            (error) =>
                @ErrorService.showErrorMessage(error)
        )

    createActorConfigurations: (configs) ->
        #TODO cover all endpoints
        endpoint = @endpoints[0]

        configurations = {
            actor: endpoint.actor.actorId,
            endpoint: endpoint.id,
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
        @$log.debug "onopen"

    onclose: (msg) =>
        @$log.debug "onclose"

    onmessage: (msg) =>
        response = angular.fromJson(msg.data)
        @$log.debug msg.data

        if response.interaction?
            @interact(response)
        else if response.status?
            @updateStatus(response)

    interact: (response) ->
        @$scope.interactionStepId = response.stepId
        @$scope.requests  = []
        @$scope.instructions = []
        for interaction in response.interaction.instructionOrRequest
            if interaction.request?
                @$scope.$apply(() =>
                    @$scope.requests.push(interaction.request)
                )
            else if interaction.instruction?
                @$scope.$apply(() =>
                    @$scope.instructions.push(interaction.instruction)
                )

        $('#provideInputModal').modal('toggle');

    updateStatus: (response) =>
        #handle loop steps
        if /\]$/.test(response.stepId) # check if step id ends with ]
            id = response.stepId.substring(0, response.stepId.lastIndexOf('['))
            if @flat[id]? && @flat[id].type == "loop"? # check if step is a loop step
                if !@iteration[response.stepId]?
                    json = @loops[id]
                    json = json.split(id+"[1]").join(response.stepId) #replace all occurences of old seq id with new one

                    step = angular.fromJson(json)

                    @flat[response.stepId] = step
                    @flatten(step.steps)

                    @iteration[response.stepId] = true

                    @$scope.$apply(() =>
                        @flat[id].sequences.push(step)
                    )

        if @flat[response.stepId]?
            if  @flat[response.stepId].status != @Constants.TEST_STATUS.COMPLETED &&
                @flat[response.stepId].status != @Constants.TEST_STATUS.ERROR &&
                @flat[response.stepId].status != @Constants.TEST_STATUS.SKIPPED
                    if  @flat[response.stepId].status == @Constants.TEST_STATUS.PROCESSING &&
                        response.status == @Constants.TEST_STATUS.WAITING
                        #do nothing
                    else
                        @$scope.$apply(() => #apply ui changes on scope
                            @flat[response.stepId].status = response.status #set status

                            if response.report?
                                @flat[response.stepId].report = response.report #set report and its path
                                @flat[response.stepId].report.path =  @session + "/" + response.stepId + ".xml"
                        )

    flatten: (steps) =>
        if steps?
            for step in steps
                @flat[step.id] = step
                @flat[step.id].status = @Constants.TEST_STATUS.WAITING

                if step.type == "decision"
                    @flatten(step.then)
                    @flatten(step.else)
                else if step.type == "flow"
                    for thread in step.threads
                        @flatten(thread)
                else if step.type == "loop"
                    seqId = step.id+"[1]"
                    json  = angular.toJson( {id: seqId, steps: step.steps, active: true } )
                    seq   = angular.fromJson(json)  #recreate the sequence
                    @flat[seqId] = seq

                    if !step.sequences?
                        step.sequences = []
                    step.sequences.push(seq)

                    @loops[step.id] = json
                    @iteration[seqId] = true # add first iteration automatically

                    for sequence in step.sequences
                        @flatten(sequence.steps)

controllers.controller('TestExecutionController', TestExecutionController)