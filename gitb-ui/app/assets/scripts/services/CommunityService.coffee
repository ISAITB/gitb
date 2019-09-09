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

  createCommunity: (shortName, fullName, email, selfRegType, selfRegToken, domainId) ->
    data = {
      community_sname: shortName,
      community_fname: fullName,
      community_email: email
    }
    if @DataService.configuration['registration.enabled']
      data.community_selfreg_type = selfRegType
      data.community_selfreg_token = selfRegToken

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

  updateCommunity: (communityId, shortName, fullName, email, selfRegType, selfRegToken, domainId) ->
    data = {
      community_sname: shortName,
      community_fname: fullName,
      community_email: email
    }
    if @DataService.configuration['registration.enabled']
      data.community_selfreg_type = selfRegType
      data.community_selfreg_token = selfRegToken

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

services.service('CommunityService', CommunityService)