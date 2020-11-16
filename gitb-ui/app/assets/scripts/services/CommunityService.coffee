class CommunityService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService', 'DataService']
  constructor: (@$log, @RestService, @DataService) ->

  getCommunities: (communityIds, skipDefault) ->
    params = {}
    params.skipDefault = skipDefault
    if communityIds? and communityIds.length > 0
        params.ids = communityIds.join ','

    @RestService.get
      path: jsRoutes.controllers.CommunityService.getCommunities().url
      authenticate: true
      params: params

  createCommunity: (shortName, fullName, email, selfRegType, selfRegRestriction, selfRegToken, selfRegTokenHelpText, selfRegNotification, description, selfRegForceTemplate, selfRegForceProperties, allowCertificateDownload, allowStatementManagement, allowSystemManagement, allowPostTestOrganisationUpdate, allowPostTestSystemUpdate, allowPostTestStatementUpdate, domainId) ->
    data = {
      community_sname: shortName,
      community_fname: fullName,
      community_email: email,
      description: description,
      allow_certificate_download: allowCertificateDownload,
      allow_statement_management: allowStatementManagement,
      allow_system_management: allowSystemManagement,
      allow_post_test_org_update: allowPostTestOrganisationUpdate,
      allow_post_test_sys_update: allowPostTestSystemUpdate,
      allow_post_test_stm_update: allowPostTestStatementUpdate
    }
    if @DataService.configuration['registration.enabled']
      if selfRegNotification == undefined
        selfRegNotification = false
      if selfRegForceTemplate == undefined
        selfRegForceTemplate = false
      if selfRegForceProperties == undefined
        selfRegForceProperties = false
      data.community_selfreg_type = selfRegType
      data.community_selfreg_token = selfRegToken
      data.community_selfreg_token_help_text = selfRegTokenHelpText
      data.community_selfreg_notification = selfRegNotification
      data.community_selfreg_force_template = selfRegForceTemplate
      data.community_selfreg_force_properties = selfRegForceProperties
      if @DataService.configuration['sso.enabled']
        data.community_selfreg_restriction = selfRegRestriction

    if domainId?
      data.domain_id = domainId

    @RestService.post({
      path: jsRoutes.controllers.CommunityService.createCommunity().url,
      data: data
    })

  getCommunityById: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.CommunityService.getCommunityById(communityId).url,
      authenticate: true
    })

  updateCommunity: (communityId, shortName, fullName, email, selfRegType, selfRegRestriction, selfRegToken, selfRegTokenHelpText, selfRegNotification, description, selfRegForceTemplate, selfRegForceProperties, allowCertificateDownload, allowStatementManagement, allowSystemManagement, allowPostTestOrganisationUpdate, allowPostTestSystemUpdate, allowPostTestStatementUpdate, domainId) ->
    data = {
      community_sname: shortName,
      community_fname: fullName,
      community_email: email,
      description: description,
      allow_certificate_download: allowCertificateDownload,
      allow_statement_management: allowStatementManagement,
      allow_system_management: allowSystemManagement,
      allow_post_test_org_update: allowPostTestOrganisationUpdate,
      allow_post_test_sys_update: allowPostTestSystemUpdate,
      allow_post_test_stm_update: allowPostTestStatementUpdate
    }
    if @DataService.configuration['registration.enabled']
      if selfRegNotification == undefined
        selfRegNotification = false
      if selfRegForceTemplate == undefined
        selfRegForceTemplate = false
      if selfRegForceProperties == undefined
        selfRegForceProperties = false
      data.community_selfreg_type = selfRegType
      data.community_selfreg_token = selfRegToken
      data.community_selfreg_token_help_text = selfRegTokenHelpText
      data.community_selfreg_notification = selfRegNotification
      data.community_selfreg_force_template = selfRegForceTemplate
      data.community_selfreg_force_properties = selfRegForceProperties
      if @DataService.configuration['sso.enabled']
        data.community_selfreg_restriction = selfRegRestriction

    if domainId?
      data.domain_id = domainId

    @RestService.post({
      path: jsRoutes.controllers.CommunityService.updateCommunity(communityId).url,
      data: data,
      authenticate: true
    })

  deleteCommunity: (communityId) ->
    @RestService.delete
      path: jsRoutes.controllers.CommunityService.deleteCommunity(communityId).url

  getUserCommunity: () ->
    @RestService.get({
      path: jsRoutes.controllers.CommunityService.getUserCommunity().url,
      authenticate: true
    })

  getSelfRegistrationOptions: () ->
    @RestService.get({
      path: jsRoutes.controllers.CommunityService.getSelfRegistrationOptions().url
    })

  selfRegister: (communityId, token, organisationShortName, organisationFullName, templateId, organisationProperties, userName, userEmail, userPassword) ->
    data = {
      community_id: communityId,
      vendor_sname: organisationShortName,
      vendor_fname: organisationFullName,
      user_name: userName,
      user_email: userEmail,
      password: userPassword,
      properties: @DataService.customPropertiesForPost(organisationProperties)
    }
    if token?
      data.community_selfreg_token = token
    if templateId?
      data.template_id = templateId

    @RestService.post({
      path: jsRoutes.controllers.CommunityService.selfRegister().url,
      data: data
    })

  getOrganisationParameters: (communityId, forFiltering) =>
    params = {}
    if forFiltering?
      filtering = forFiltering
    @RestService.get({
      path: jsRoutes.controllers.CommunityService.getOrganisationParameters(communityId).url
      authenticate: true
      params: params
    })

  getSystemParameters: (communityId, forFiltering) =>
    params = {}
    if forFiltering?
      filtering = forFiltering
    @RestService.get({
      path: jsRoutes.controllers.CommunityService.getSystemParameters(communityId).url
      authenticate: true
      params: params
    })

  deleteOrganisationParameter: (parameterId) =>
    @RestService.delete
      path: jsRoutes.controllers.CommunityService.deleteOrganisationParameter(parameterId).url

  deleteSystemParameter: (parameterId) =>
    @RestService.delete
      path: jsRoutes.controllers.CommunityService.deleteSystemParameter(parameterId).url

  createOrganisationParameter: (parameter) =>
    data = {
      name: parameter.name,
      test_key: parameter.testKey,
      description: parameter.desc,
      use: parameter.use,
      kind: parameter.kind,
      admin_only: parameter.adminOnly,
      not_for_tests: parameter.notForTests,
      in_exports: parameter.inExports,
      in_selfreg: parameter.inSelfRegistration,
      hidden: parameter.hidden,
      allowedValues: parameter.allowedValues,
      dependsOn: parameter.dependsOn,
      dependsOnValue: parameter.dependsOnValue,
      community_id: parameter.community
    }
    @RestService.post({
      path: jsRoutes.controllers.CommunityService.createOrganisationParameter().url,
      data: data,
      authenticate: true
    })

  createSystemParameter: (parameter) =>
    data = {
      name: parameter.name,
      test_key: parameter.testKey,
      description: parameter.desc,
      use: parameter.use,
      kind: parameter.kind,
      admin_only: parameter.adminOnly,
      not_for_tests: parameter.notForTests,
      in_exports: parameter.inExports,
      hidden: parameter.hidden,
      allowedValues: parameter.allowedValues,
      dependsOn: parameter.dependsOn,
      dependsOnValue: parameter.dependsOnValue,
      community_id: parameter.community
    }
    @RestService.post({
      path: jsRoutes.controllers.CommunityService.createSystemParameter().url,
      data: data,
      authenticate: true
    })

  updateOrganisationParameter: (parameter) =>
    data = {
      id: parameter.id
      name: parameter.name,
      test_key: parameter.testKey,
      description: parameter.desc,
      use: parameter.use,
      kind: parameter.kind,
      admin_only: parameter.adminOnly,
      not_for_tests: parameter.notForTests,
      in_exports: parameter.inExports,
      in_selfreg: parameter.inSelfRegistration,
      hidden: parameter.hidden,
      allowedValues: parameter.allowedValues,
      dependsOn: parameter.dependsOn,
      dependsOnValue: parameter.dependsOnValue,
      community_id: parameter.community
    }
    @RestService.post({
      path: jsRoutes.controllers.CommunityService.updateOrganisationParameter(parameter.id).url,
      data: data,
      authenticate: true
    })

  orderOrganisationParameters: (community, orderedIds) =>
    data = {
      ids: orderedIds.join ','
    }
    @RestService.post({
      path: jsRoutes.controllers.CommunityService.orderOrganisationParameters(community).url,
      data: data,
      authenticate: true
    })

  orderSystemParameters: (community, orderedIds) =>
    data = {
      ids: orderedIds.join ','
    }
    @RestService.post({
      path: jsRoutes.controllers.CommunityService.orderSystemParameters(community).url,
      data: data,
      authenticate: true
    })

  updateSystemParameter: (parameter) =>
    data = {
      id: parameter.id
      name: parameter.name,
      test_key: parameter.testKey,
      description: parameter.desc,
      use: parameter.use,
      kind: parameter.kind,
      admin_only: parameter.adminOnly,
      not_for_tests: parameter.notForTests,
      in_exports: parameter.inExports,
      hidden: parameter.hidden,
      allowedValues: parameter.allowedValues,
      dependsOn: parameter.dependsOn,
      dependsOnValue: parameter.dependsOnValue,
      community_id: parameter.community
    }
    @RestService.post({
      path: jsRoutes.controllers.CommunityService.updateSystemParameter(parameter.id).url,
      data: data,
      authenticate: true
    })

  getCommunityLabels: (communityId) =>
    @RestService.get({
      path: jsRoutes.controllers.CommunityService.getCommunityLabels(communityId).url
      authenticate: true
    })    

  setCommunityLabels: (communityId, labels) =>
    data = {
      values: labels
    }
    @RestService.post({
      path: jsRoutes.controllers.CommunityService.setCommunityLabels(communityId).url,
      data: data,
      authenticate: true
    })

  exportCommunity: (communityId, settings) =>
    data = {
      values: settings
    }
    @RestService.post({
      path: jsRoutes.controllers.RepositoryService.exportCommunity(communityId).url,
      data: data,
      authenticate: true,
      responseType: "arraybuffer"
    })

  uploadCommunityExport: (communityId, settings, archiveData) =>
    data = {
      settings: settings
      data: archiveData
    }
    @RestService.post({
      path: jsRoutes.controllers.RepositoryService.uploadCommunityExport(communityId).url,
      data: data,
      authenticate: true
    })

  cancelCommunityImport: (communityId, pendingImportId) =>
    data = {
      pending_id: pendingImportId
    }
    @RestService.post({
      path: jsRoutes.controllers.RepositoryService.cancelCommunityImport(communityId).url,
      data: data,
      authenticate: true
    })

  confirmCommunityImport: (communityId, pendingImportId, settings, items) =>
    data = {
      settings: settings
      pending_id: pendingImportId
      items: items
    }
    if @DataService.isCommunityAdmin
      path = jsRoutes.controllers.RepositoryService.confirmCommunityImportCommunityAdmin(communityId).url
    else
      path = jsRoutes.controllers.RepositoryService.confirmCommunityImportTestBedAdmin(communityId).url
    @RestService.post({
      path: path,
      data: data,
      authenticate: true
    })

services.service('CommunityService', CommunityService)