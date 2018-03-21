class LandingPageService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  getLandingPagesByCommunity: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.LandingPageService.getLandingPagesByCommunity(communityId).url,
      authenticate: true
    })

  createLandingPage: (name, description, content, defaultFlag, communityId) ->
    @RestService.post({
      path: jsRoutes.controllers.LandingPageService.createLandingPage().url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      }
      authenticate: true
    })

  getLandingPageById: (pageId) ->
    @RestService.get({
      path: jsRoutes.controllers.LandingPageService.getLandingPageById(pageId).url,
      authenticate: true
    })

  updateLandingPage: (pageId, name, description, content, defaultFlag, communityId) ->
    @RestService.post({
      path: jsRoutes.controllers.LandingPageService.updateLandingPage(pageId).url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      }
      authenticate: true
    })

  deleteLandingPage: (pageId) ->
    @RestService.delete
      path: jsRoutes.controllers.LandingPageService.deleteLandingPage(pageId).url

  getCommunityDefaultLandingPage: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.LandingPageService.getCommunityDefaultLandingPage().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })

services.service('LandingPageService', LandingPageService)