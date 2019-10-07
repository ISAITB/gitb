class TestService

    @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
    @defaultConfig = { headers: @headers }

    @$inject = ['$log', 'RestService']
    constructor: (@$log, @RestService) ->
        @$log.debug "Constructing TestService..."

    getTestCaseDefinition: (testCase) ->
        @$log.debug "calling with #{testCase}"
        @RestService.get({
            path: jsRoutes.controllers.TestService.getTestCaseDefinition(testCase).url,
            authenticate: true
        })

    getActorDefinitions: (specificationId) ->
        @RestService.get({
            path: jsRoutes.controllers.TestService.getActorDefinitions().url,
            authenticate: true
            params:
                spec_id: specificationId
        })

    initiate: (testCase) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.initiate(testCase).url,
            authenticate: true
        })

    configure: (specId, session, systemId, actorId) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.configure(session).url,
            params:
                spec_id: specId
                system_id: systemId
                actor_id: actorId
            authenticate: true
        })

    provideInput: (session, step, inputs) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.provideInput(session).url,
            data: {
                teststep: step,
                inputs: angular.toJson(inputs)
            },
            authenticate: true
        })

    initiatePreliminary: (session) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.initiatePreliminary(session).url,
            authenticate: true
        })

    start: (session) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.start(session).url,
            authenticate: true
        })

    stop: (session) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.stop(session).url,
            authenticate: true
        })

    restart: (session) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.restart(session).url,
            authenticate: true
        })
    
    getBinaryMetadata: (data, isBase64) ->
        base64 = isBase64? && isBase64
        @RestService.post({
            path: jsRoutes.controllers.TestResultService.getBinaryMetadata().url,
            authenticate: true
            data: {
                data: data
                is_base64: base64
            }            
        })

services.service('TestService', TestService)