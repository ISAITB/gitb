class SystemService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService', 'DataService']
  constructor: (@$log, @RestService, @DataService) ->
    @$log.debug "Constructing SystemService..."

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

  updateSystem:(systemId, sname, fname, description, version, organisationId, otherSystem, processProperties, properties, copySystemParameters, copyStatementParameters) ->
    data = {}
    if sname?
      data.system_sname = sname
    if fname?
      data.system_fname = fname
    if description?
      data.system_description = description
    if version?
      data.system_version = version
    if otherSystem? && otherSystem.id?
      data.other_system = otherSystem.id
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    if processProperties
      data.properties = @DataService.customPropertiesForPost(properties)

    data.organization_id = organisationId

    @RestService.post({
      path: jsRoutes.controllers.SystemService.updateSystemProfile(systemId).url,
      data: data,
      authenticate: true
    })

  registerSystemWithOrganization:(sname, fname, description, version, orgId, otherSystem, processProperties, properties, copySystemParameters, copyStatementParameters) ->
    data = {
      system_sname: sname,
      system_fname: fname,
      system_version: version
      organization_id: orgId
    }
    if otherSystem? && otherSystem.id?
      data.other_system = otherSystem.id
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters

    if description?
      data.system_description = description
    if processProperties
      data.properties = @DataService.customPropertiesForPost(properties)

    @RestService.post({
      path: jsRoutes.controllers.SystemService.registerSystemWithOrganization().url,
      data: data,
      authenticate: true
    })

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

  getEndpointConfigurations: (endpoint, system) ->
    @RestService.get
      path: jsRoutes.controllers.SystemService.getEndpointConfigurations(endpoint).url
      params:
        system_id: system
      authenticate: true

  deleteEndpointConfiguration: (systemId, parameterId, endpointId) ->
    @RestService.delete
      path: jsRoutes.controllers.SystemService.saveEndpointConfiguration(endpointId).url
      authenticate: true
      params:
        system_id: systemId
        parameter_id: parameterId

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

  deleteConformanceStatement: (system, actorIds) ->
    @RestService.delete
      path: jsRoutes.controllers.SystemService.deleteConformanceStatement(system).url
      params:
        ids: actorIds.join ','

  deleteSystem: (systemId, organisationId) ->
    @RestService.delete
      path: jsRoutes.controllers.SystemService.deleteSystem(systemId).url
      params: 
        organization_id: organisationId

  getSystems: (systemIds) =>
    params = {}
    if systemIds? and systemIds.length > 0
        params.ids = systemIds.join ','

    @RestService.get
      path: jsRoutes.controllers.SystemService.getSystems().url
      authenticate: true
      params: params

  getSystemsByCommunity: () =>
    @RestService.get
      path: jsRoutes.controllers.SystemService.getSystemsByCommunity(@DataService.community.id).url
      authenticate: true

  getSystemParameterValues: (systemId, includeValues) ->
    params = {}
    if includeValues?
      params.values = includeValues
    @RestService.get({
      path: jsRoutes.controllers.SystemService.getSystemParameterValues(systemId).url,
      authenticate: true
      params: params
    })

services.service('SystemService', SystemService)