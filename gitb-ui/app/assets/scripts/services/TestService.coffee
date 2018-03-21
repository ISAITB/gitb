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

    getActorDefinition: (actor) ->
        @RestService.get({
            path: jsRoutes.controllers.TestService.getActorDefinition(actor).url,
            authenticate: true
        })

    initiate: (testCase) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.initiate(testCase).url,
            authenticate: true
        })

    configure: (session, configs) ->
        @RestService.post({
            path: jsRoutes.controllers.TestService.configure(session).url,
            data: {
                configs: angular.toJson(configs)
            }
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

    getSessions: () ->
        @RestService.get({
            path: jsRoutes.controllers.TestService.getSessions().url,
            authenticate: true
        })

services.service('TestService', TestService)