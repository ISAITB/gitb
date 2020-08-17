class ConformanceService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService', '$upload', 'DataService']

  constructor: (@$log, @RestService, @$upload, @DataService) ->
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

  getDomainsForSystem: (systemId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getDomainsForSystem(systemId).url,
      authenticate: true
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

  createEndpoint: (name, description, actor) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createEndpoint().url
      authenticate: true
      data:
        name: name
        description: description
        actor_id: actor
    })

  createParameter: (name, description, use, kind, adminOnly, notForTests, endpointId) ->
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.createParameter().url
      authenticate: true
      data:
        name: name
        description: description
        use: use
        kind: kind
        admin_only: adminOnly
        not_for_tests: notForTests
        endpoint_id: endpointId
    })

  getSystemConfigurations: (actorId, systemId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getSystemConfigurations().url,
      authenticate: true
      params: {
        actor_id: actorId,
        system_id: systemId
      }
    })

  checkConfigurations: (actorId, systemId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.checkConfigurations().url,
      authenticate: true
      params: {
        actor_id: actorId,
        system_id: systemId
      }
    })

  createActor: (shortName, fullName, description, defaultActor, hiddenActor, displayOrder, domainId, specificationId) =>
    if hiddenActor == undefined
      hiddenActor = false
    data = {
        actor_id: shortName
        name: fullName
        description: description
        default: defaultActor
        hidden: hiddenActor
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

  createSpecification: (shortName, fullName, description, hidden, domainId) ->
    if hidden == undefined
      hidden = false  
    @RestService.post 
      path: jsRoutes.controllers.ConformanceService.createSpecification().url
      authenticate: true
      data:
        sname: shortName
        fname: fullName
        description: description
        hidden: hidden
        domain_id: domainId

  getSpecificationsWithIds: (ids) =>
    params = {}

    if ids?
      params['ids'] = ids.join ","

    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getSpecs().url,
      authenticate: true
      params: params
    })

  getSpecificationsForSystem: (systemId) =>
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getSpecsForSystem(systemId).url,
      authenticate: true
    })

  getActorsForDomain: (domainId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getActorsForDomain(domainId).url,
      authenticate: true
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

  getSpecifications: (domainId) =>
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
    }
    if domainParameterKind == 'BINARY'
      params.valueBinary = domainParameterValue
    else
      params.value = domainParameterValue
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
    }
    if domainParameterKind == 'BINARY'
      params.valueBinary = domainParameterValue
    else
      params.value = domainParameterValue
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

  deployTestSuite: (specificationIds, file) ->
    if file?
      options =
        url: jsRoutes.controllers.ConformanceService.deployTestSuiteToSpecifications().url.substring(1)
        file: file
        data: {
          specification_ids: specificationIds.join ','
        }
      @$upload.upload options
    else
      null

  resolvePendingTestSuite: (pendingFolderId, overallAction, specificationIds, specificationActions) ->
    data = {
        pending_id: pendingFolderId
        pending_action: overallAction
        specification_ids: specificationIds.join ','
    }
    if specificationActions?
      data.actions = JSON.stringify(specificationActions)
    @RestService.post({
      path: jsRoutes.controllers.ConformanceService.resolvePendingTestSuites().url,
      authenticate: true
      data: data
    })

  getTestSuiteDocumentation: (id) ->
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getTestSuiteDocumentation(id).url
      authenticate: true

  getTestCaseDocumentation: (id) ->
    @RestService.get
      path: jsRoutes.controllers.ConformanceService.getTestCaseDocumentation(id).url
      authenticate: true

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

  getConformanceOverview: (domainIds, specIds, actorIds, communityIds, organizationIds, systemIds, fullResults, forExport) ->
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

    params.export = forExport? && forExport

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

  getTestSuiteTestCase: (testCaseId) ->
    @RestService.get({
      path: jsRoutes.controllers.ConformanceService.getTestSuiteTestCase(testCaseId).url,
      authenticate: true
    })

  exportDomain: (domainId, settings) =>
    data = {
      values: settings
    }
    @RestService.post({
      path: jsRoutes.controllers.RepositoryService.exportDomain(domainId).url,
      data: data,
      authenticate: true,
      responseType: "arraybuffer"
    })

  uploadDomainExport: (domainId, settings, archiveData) =>
    data = {
      settings: settings
      data: archiveData
    }
    @RestService.post({
      path: jsRoutes.controllers.RepositoryService.uploadDomainExport(domainId).url,
      data: data,
      authenticate: true
    })

  cancelDomainImport: (domainId, pendingImportId) =>
    data = {
      pending_id: pendingImportId
    }
    @RestService.post({
      path: jsRoutes.controllers.RepositoryService.cancelDomainImport(domainId).url,
      data: data,
      authenticate: true
    })

  confirmDomainImport: (domainId, pendingImportId, settings, items) =>
    data = {
      settings: settings
      pending_id: pendingImportId
      items: items
    }
    if @DataService.isCommunityAdmin
      path = jsRoutes.controllers.RepositoryService.confirmDomainImportCommunityAdmin(domainId).url
    else
      path = jsRoutes.controllers.RepositoryService.confirmDomainImportTestBedAdmin(domainId).url
    @RestService.post({
      path: path,
      data: data,
      authenticate: true
    })

services.service('ConformanceService', ConformanceService)