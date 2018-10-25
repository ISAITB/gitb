class ConformanceService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService', '$upload']

  constructor: (@$log, @RestService, @$upload) ->
    @$log.debug "Constructing ConformanceService..."

  getDomains: (ids) ->
    params = {}

    if ids? and ids.length > 0
      params['ids'] = ids.join ","

    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getDomains().url,
      authenticate: true
      params: params
    })

  getDomainForSpecification: (specId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getDomainOfSpecification(specId).url,
      authenticate: true
    })

  getCommunityDomain: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getCommunityDomain().url,
      authenticate: true
      params: {
        community_id: communityId
      }
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

  createEndpoint: (name, description, actor) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createEndpoint().url
      authenticate: true
      data:
        name: name
        description: description
        actor_id: actor
    })

  createParameter: (name, description, use, kind, endpointId) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createParameter().url
      authenticate: true
      data:
        name: name
        description: description
        use: use
        kind: kind
        endpoint_id: endpointId
    })

  createActor: (shortName, fullName, description, defaultActor, displayOrder, domainId, specificationId) =>
    data = {
        actor_id: shortName
        name: fullName
        description: description
        default: defaultActor
        domain_id: domainId
        spec_id: specificationId
    }
    if displayOrder? && (!displayOrder.trim? || displayOrder.trim() != '')
      data.displayOrder = Number(displayOrder)
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createActor().url
      authenticate: true
      data: data
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

  getDomainParameters: (domainId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getDomainParameters(domainId).url,
      authenticate: true
    })

  updateDomainParameter: (domainParameterId, domainParameterName, domainParameterDescription, domainParameterValue, domainParameterKind, domainId) ->
    params = {
      name: domainParameterName,
      desc: domainParameterDescription,
      kind: domainParameterKind,
      value: domainParameterValue
    }
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.updateDomainParameter(domainId, domainParameterId).url,
      authenticate: true,
      data:
        config: angular.toJson params
    })

  createDomainParameter: (domainParameterName, domainParameterDescription, domainParameterValue, domainParameterKind, domainId) ->
    params = {
      name: domainParameterName,
      desc: domainParameterDescription,
      kind: domainParameterKind,
      value: domainParameterValue
    }
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createDomainParameter(domainId).url,
      authenticate: true,
      data:
        config: angular.toJson params
    })

  deleteDomainParameter: (domainParameterId, domainId) ->
    @RestService.delete({
      path: jsRoutes.controllers.ConformanceService.deleteDomainParameter(domainId, domainParameterId).url,
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
        url: jsRoutes.controllers.ConformanceService.deployTestSuite(specificationId).url.substring(1)
        file: file
      @$upload.upload options
    else
      null

  resolvePendingTestSuite: (specificationId, pendingFolderId, action) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.resolvePendingTestSuite(specificationId).url,
      authenticate: true
      data: {
        pending_id: pendingFolderId
        pending_action: action
      }
    })

  getTestSuites: (specificationId) ->
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getSpecTestSuites(specificationId).url
      authenticate: true

  getConformanceStatus: (actorId, sutId) ->
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getConformanceStatus(actorId, sutId).url
      authenticate: true

  getConformanceStatusForTestSuite: (actorId, sutId, testSuiteId) ->
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getConformanceStatusForTestSuite(actorId, sutId, testSuiteId).url
      authenticate: true

  getTestSuiteTestCase: (testCaseId) ->
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getTestSuiteTestCase(testCaseId).url
      authenticate: true

  getConformanceOverview: (domainIds, specIds, actorIds, communityIds, organizationIds, systemIds, fullResults) ->
    params = {}
    params.full = fullResults
    if domainIds? and domainIds.length > 0
      params.domain_ids = domainIds.join ','
    if specIds? and specIds.length > 0
      params.specification_ids = specIds.join ','
    if actorIds? and actorIds.length > 0
      params.actor_ids = actorIds.join ','
    if communityIds? and communityIds.length > 0
      params.community_ids = communityIds.join ','
    if organizationIds? and organizationIds.length > 0
      params.organization_ids = organizationIds.join ','
    if systemIds? and systemIds.length > 0
      params.system_ids = systemIds.join ','

    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getConformanceOverview().url
      authenticate: true
      params: params

  deleteObsoleteTestResultsForCommunity: (communityId) ->
    @RestService.delete
      path: jsRoutes.controllers.ConformanceService.deleteObsoleteTestResultsForCommunity().url
      authenticate: true
      params: {
        community_id: communityId
      }

  deleteObsoleteTestResults: () ->
    @RestService.delete
      path: jsRoutes.controllers.ConformanceService.deleteAllObsoleteTestResults().url
      authenticate: true

  deleteObsoleteTestResultsForSystem: (systemId) ->
    @RestService.delete
      path: jsRoutes.controllers.ConformanceService.deleteObsoleteTestResultsForSystem().url
      authenticate: true
      params: {
        system_id: systemId
      }

  getConformanceCertificateSettings: (communityId, includeKeystoreData) =>
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getConformanceCertificateSettings(communityId).url
      authenticate: true
      params: {
        keystore: includeKeystoreData
      }

  updateConformanceCertificateSettings: (communityId, settings, updatePasswords, removeKeystore) =>
    data = {}
    if settings?
      data.title = settings.title
      data.message = settings.message
      data.includeMessage = settings.includeMessage? && settings.includeMessage
      data.includeTestStatus = settings.includeTestStatus? && settings.includeTestStatus
      data.includeTestCases = settings.includeTestCases? && settings.includeTestCases
      data.includeDetails = settings.includeDetails? && settings.includeDetails
      data.includeSignature = settings.includeSignature? && settings.includeSignature
      data.keystoreFile = settings.keystoreFile
      data.keystoreType = settings.keystoreType
      data.keystorePassword = settings.keystorePassword
      data.keyPassword = settings.keyPassword
    @RestService.post
      path: jsRoutes.controllers.ConformanceService.updateConformanceCertificateSettings(communityId).url
      authenticate: true
      data: {
        settings: angular.toJson data
        updatePasswords: updatePasswords
        removeKeystore: removeKeystore
      }

  testKeystoreSettings: (communityId, settings, updatePasswords) =>
    data = {}
    if settings?
      data.keystoreFile = settings.keystoreFile
      data.keystoreType = settings.keystoreType
      data.keystorePassword = settings.keystorePassword
      data.keyPassword = settings.keyPassword
    @RestService.post
      path: jsRoutes.controllers.ConformanceService.testKeystoreSettings(communityId).url
      authenticate: true
      data: {
        settings: angular.toJson data
        updatePasswords: updatePasswords
      }

  exportDemoConformanceCertificateReport: (communityId, settings) ->
    data = {}
    if settings?
      data.title = settings.title
      data.includeMessage = settings.includeMessage? && settings.includeMessage
      data.includeTestStatus = settings.includeTestStatus? && settings.includeTestStatus
      data.includeTestCases = settings.includeTestCases? && settings.includeTestCases
      data.includeDetails = settings.includeDetails? && settings.includeDetails
      data.includeSignature = settings.includeSignature? && settings.includeSignature
      if data.includeMessage
        data.message = settings.message
      if data.includeSignature
        data.keystoreFile = settings.keystoreFile
        data.keystoreType = settings.keystoreType
        data.keystorePassword = settings.keystorePassword
        data.keyPassword = settings.keyPassword
    @RestService.post
      path: jsRoutes.controllers.RepositoryService.exportDemoConformanceCertificateReport(communityId).url
      data: {
        settings: angular.toJson data
      }
      authenticate: true
      responseType: "arraybuffer"

  exportConformanceCertificateReport: (communityId, actorId, systemId, settings) ->
    data = {}
    if settings?
      data.title = settings.title
      data.includeMessage = settings.includeMessage? && settings.includeMessage
      data.includeTestStatus = settings.includeTestStatus? && settings.includeTestStatus
      data.includeTestCases = settings.includeTestCases? && settings.includeTestCases
      data.includeDetails = settings.includeDetails? && settings.includeDetails
      data.includeSignature = settings.includeSignature? && settings.includeSignature
      if data.includeMessage
        data.message = settings.message
    @RestService.post
      path: jsRoutes.controllers.RepositoryService.exportConformanceCertificateReport().url
      data: {
        settings: angular.toJson data
        community_id: communityId
        actor_id: actorId
        system_id: systemId
      }
      authenticate: true
      responseType: "arraybuffer"

services.service('ConformanceService', ConformanceService)
