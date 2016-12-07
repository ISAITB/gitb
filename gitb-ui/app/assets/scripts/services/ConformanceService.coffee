class ConformanceService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService', '$upload']

  constructor: (@$log, @RestService, @$upload) ->
    @$log.debug "Constructing ConformanceService..."

  getDomains: (ids) ->
    params = {}

    if ids?
      params['ids'] = ids.join ","

    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getDomains().url,
      authenticate: true
      params: params
    })

  deleteDomain: (domainId) ->
    @RestService.delete
      path: jsRoutes.controllers.ConformanceService.deleteDomain(domainId).url
      authenticate: true

  updateDomain: (domainId, shortName, fullName, description) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.updateDomain(domainId).url,
      data: {
        sname: shortName
        fname: fullName
        description: description
      }
      authenticate: true
    })
  
  createDomain: (shortName, fullName, description) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createDomain().url
      authenticate: true
      data:
        sname: shortName
        fname: fullName
        description: description
    })
  
  createOption: (shortName, fullName, description, actor) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createOption().url
      authenticate: true
      data:
        sname: shortName
        fname: fullName
        description: description
        actor: actor
    })
  
  createActor: (shortName, fullName, description, domainId, specificationId) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createActor().url
      authenticate: true
      data:
        sname: shortName
        fname: fullName
        description: description
        domain_id: domainId
        spec_id: specificationId
    })

  createSpecification: (shortName, fullName, urls, diagram, description, specificationType, domainId) ->
    @RestService.post 
      path: jsRoutes.controllers.ConformanceService.createSpecification().url
      authenticate: true
      data:
        sname: shortName
        fname: fullName
        urls: urls
        diagram: diagram
        description: description
        spec_type: specificationType
        domain_id: domainId

  getSpecificationsWithIds: (ids)->
    params = {}

    if ids?
      params['ids'] = ids.join ","

    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getSpecs().url,
      authenticate: true
      params: params
    })

  getActorsWithIds: (ids)->
    params = {}

    if ids?
      params['ids'] = ids.join ","

    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getActors().url,
      authenticate: true
      params: params
    })

  getSpecifications: (domainId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getDomainSpecs(domainId).url,
      authenticate: true
    })

  getActorsWithSpecificationId: (specId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getSpecActors(specId).url,
      authenticate: true
    })

  getActorsWithDomainId: (domainId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getDomainActors(domainId).url,
      authenticate: true
    })

  getTestCases: (actorId, specId, optionIds, type) ->
    params = {
      spec: specId,
      type: type
    }

    if optionIds? and optionIds.length > 0
        params.options = optionIds.join ','

    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getActorTestCases(actorId).url
      authenticate: true
      params: params

  getOptionsForActor: (actorId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getOptionsForActor(actorId).url,
      authenticate: true
    })

  getEndpointsForActor: (actorId) ->
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getEndpointsForActor(actorId).url
      authenticate: true

  getEndpoints: (endpointIds) ->

    params = {}
    if endpointIds?
      params["ids"] = endpointIds.join ","

    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getEndpoints().url,
      authenticate: true
      params: params
    })

  getOptions: (optionIds) ->

    params = {}
    if optionIds?
      params["ids"] = optionIds.join ","

    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getOptions().url,
      authenticate: true
      params: params
    })

  addActorToSpecification: (specificationId, actorId) ->
    @RestService.post
      path: jsRoutes.controllers.ConformanceService.addActorToSpecification(specificationId, actorId).url
      authenticate: true

  deployTestSuite: (specificationId, file) ->
    if file?
      options =
        url: jsRoutes.controllers.ConformanceService.deployTestSuite(specificationId).url
        file: file
      @$upload.upload options
    else
      null

  getTestSuites: (specificationId) ->
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getSpecTestSuites(specificationId).url
      authenticate: true



services.service('ConformanceService', ConformanceService)
