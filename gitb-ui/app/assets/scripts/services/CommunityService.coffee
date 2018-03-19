class CommunityService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  getCommunities: (communityIds) ->
    params = {}
    if communityIds? and communityIds.length > 0
        params.ids = communityIds.join ','

    @RestService.get
      path: jsRoutes.controllers.CommunityService.getCommunities().url
      authenticate: true
      params: params

  createCommunity: (shortName, fullName, domainId) ->
    data = {
      community_sname: shortName,
      community_fname: fullName
    }

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

  updateCommunity: (communityId, shortName, fullName, domainId) ->
    data = {
      community_sname: shortName,
      community_fname: fullName
    }

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

services.service('CommunityService', CommunityService)