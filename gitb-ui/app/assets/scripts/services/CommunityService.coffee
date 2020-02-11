class CommunityService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService', 'DataService']
  constructor: (@$log, @RestService, @DataService) ->

  getCommunities: (communityIds) ->
    params = {}
    if communityIds? and communityIds.length > 0
        params.ids = communityIds.join ','

    @RestService.get
      path: jsRoutes.controllers.CommunityService.getCommunities().url
      authenticate: true
      params: params

  createCommunity: (shortName, fullName, email, selfRegType, selfRegToken, selfRegNotification, domainId) ->
    data = {
      community_sname: shortName,
      community_fname: fullName,
      community_email: email
    }
    if @DataService.configuration['registration.enabled']
      if selfRegNotification == undefined
        selfRegNotification = false
      data.community_selfreg_type = selfRegType
      data.community_selfreg_token = selfRegToken
      data.community_selfreg_notification = selfRegNotification

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

  updateCommunity: (communityId, shortName, fullName, email, selfRegType, selfRegToken, selfRegNotification, domainId) ->
    data = {
      community_sname: shortName,
      community_fname: fullName,
      community_email: email
    }
    if @DataService.configuration['registration.enabled']
      if selfRegNotification == undefined
        selfRegNotification = false
      data.community_selfreg_type = selfRegType
      data.community_selfreg_token = selfRegToken
      data.community_selfreg_notification = selfRegNotification

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

  selfRegister: (communityId, token, organisationShortName, organisationFullName, templateId, userName, userEmail, userPassword) ->
    data = {
      community_id: communityId,
      vendor_sname: organisationShortName,
      vendor_fname: organisationFullName,
      user_name: userName,
      user_email: userEmail,
      password: userPassword
    }
    if token?
      data.community_selfreg_token = token
    if templateId?
      data.template_id = templateId

    @RestService.post({
      path: jsRoutes.controllers.CommunityService.selfRegister().url,
      data: data
    })

  getOrganisationParameters: (communityId) =>
    @RestService.get({
      path: jsRoutes.controllers.CommunityService.getOrganisationParameters(communityId).url
      authenticate: true
    })

  getSystemParameters: (communityId) =>
    @RestService.get({
      path: jsRoutes.controllers.CommunityService.getSystemParameters(communityId).url
      authenticate: true
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
      community_id: parameter.community
    }
    @RestService.post({
      path: jsRoutes.controllers.CommunityService.updateOrganisationParameter(parameter.id).url,
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

services.service('CommunityService', CommunityService)