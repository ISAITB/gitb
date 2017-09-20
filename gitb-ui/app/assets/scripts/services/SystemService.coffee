class SystemService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  constructor: (@$log, @RestService) ->
    @$log.debug "Constructing SystemService..."


  #dummy method for getting json
  getTestSteps: () ->
    @RestService.get({
      path: "/assets/jsons/execution.json"
    })

  #dummy method for getting json
  getTestSuites: () ->
    @RestService.get({
      path: "/assets/jsons/testsuites.json"
    })

  getVendorSystems: () ->
    @RestService.get({
      path: jsRoutes.controllers.SystemService.getVendorSystems().url,
      authenticate: true
    })

  getSystemsByOrganization: (orgId) ->
    @RestService.get({
      path: jsRoutes.controllers.SystemService.getSystemsByOrganization().url,
      authenticate: true,
      params: {
        organization_id: orgId
      }
    })

  getSystem:(systemId) ->
    @RestService.get({
      path: jsRoutes.controllers.SystemService.getSystemProfile(systemId).url,
      authenticate: true
    })

  updateSystem:(systemId, sname, fname, description, version) ->
    data = {}

    if sname?
      data.system_sname = sname
    if fname?
      data.system_fname = fname
    if description?
      data.system_description = description
    if version?
      data.system_version = version

    @RestService.post({
      path: jsRoutes.controllers.SystemService.updateSystemProfile(systemId).url,
      data: data,
      authenticate: true
    })

  registerSystem:(sname, fname, description, version) ->
    data = {
      system_sname: sname,
      system_fname: fname,
      system_version: version
    }

    if description?
      data.system_description = description

    @RestService.post({
      path: jsRoutes.controllers.SystemService.registerSystem().url,
      data: data,
      authenticate: true
    })

  registerSystemWithOrganization:(sname, fname, description, version, orgId) ->
    data = {
      system_sname: sname,
      system_fname: fname,
      system_version: version
      organization_id: orgId
    }

    if description?
      data.system_description = description

    @RestService.post({
      path: jsRoutes.controllers.SystemService.registerSystemWithOrganization().url,
      data: data,
      authenticate: true
    })

  getImplementedActors: (system)->
    @RestService.get 
      path: jsRoutes.controllers.SystemService.getImplementedActors(system).url
      authenticate: true

  getConformanceStatements: (system, specId, actorId) ->
    if actorId? and specId?
      @RestService.get
        path: jsRoutes.controllers.SystemService.getConformanceStatements(system).url
        authenticate: true
        params: 
          spec: specId
          actor: actorId
    else
      @RestService.get({
        path: jsRoutes.controllers.SystemService.getConformanceStatements(system).url,
        authenticate: true
      })


  defineConformanceStatement: (system, spec, actor, options) ->
    data = {
      spec: spec,
      actor: actor
    }

    if options? and options.length > 0
      data.options = options.join ','

    @RestService.post({
      path: jsRoutes.controllers.SystemService.defineConformanceStatement(system).url,
      data: data,
      authenticate: true
    })

  ###
  getSystemConfigurations: (system) ->
    @RestService.get({
      path: jsRoutes.controllers.SystemService.getSystemConfigurations(system).url,
      authenticate: true
    })

  saveSystemConfigurations: (system, configs) =>
    @RestService.post({
      path: jsRoutes.controllers.SystemService.updateSystemConfigurations(system).url,
      data: {
          configs: angular.toJson(configs)
      },
      authenticate: true
    })
  ###

  getEndpointConfigurations: (endpoint, system) ->
    @RestService.get
      path: jsRoutes.controllers.SystemService.getEndpointConfigurations(endpoint).url
      params:
        system_id: system
      authenticate: true

  saveEndpointConfiguration: (endpoint, config) ->
    @RestService.post
      path: jsRoutes.controllers.SystemService.saveEndpointConfiguration(endpoint).url
      authenticate: true
      data:
        config: angular.toJson config

  getConfigurationsWithEndpointIds: (endpointIds, systemId) ->
    @RestService.get
      path: jsRoutes.controllers.SystemService.getConfigurationsWithEndpointIds().url
      authenticate: true
      params:
        ids: endpointIds.join ','
        system_id: systemId

  getLastExecutionResultsForTestCases: (system, testCaseIds) ->
    @RestService.get
      path: jsRoutes.controllers.SystemService.getLastExecutionResultsForTestCases(system).url
      params:
        ids: testCaseIds.join ','

  deleteConformanceStatement: (system, actorIds) ->
    @RestService.delete
      path: jsRoutes.controllers.SystemService.deleteConformanceStatement(system).url
      params:
        ids: actorIds.join ','

  getLastExecutionResultsForTestSuite: (system, testSuiteId, testCaseIds) ->
      @RestService.get
        path: jsRoutes.controllers.SystemService.getLastExecutionResultsForTestSuite(system).url
        params:
          id: testSuiteId,
          ids: testCaseIds.join ','

  getSystems: (systemIds) ->
    params = {}
    if systemIds? and systemIds.length > 0
        params.ids = systemIds.join ','

    @RestService.get
      path: jsRoutes.controllers.SystemService.getSystems().url
      authenticate: true
      params: params

services.service('SystemService', SystemService)