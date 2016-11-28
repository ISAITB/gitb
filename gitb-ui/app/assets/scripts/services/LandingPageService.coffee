class LandingPageService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

  getLandingPages: () ->
    @RestService.get({
      path: jsRoutes.controllers.LandingPageService.getLandingPages().url,
      authenticate: true
    })

  getLandingPageById: (pageId) ->
    @RestService.get({
      path: jsRoutes.controllers.LandingPageService.getLandingPageById(pageId).url,
      authenticate: true
    })

  getDefaultLandingPage: () ->
    @RestService.get({
      path: jsRoutes.controllers.LandingPageService.getDefaultLandingPage().url,
      authenticate: true
    })

  createLandingPage: (name, description, content, defaultFlag) ->
    @RestService.post({
      path: jsRoutes.controllers.LandingPageService.createLandingPage().url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag
      }
      authenticate: true
    })

  updateLandingPage: (pageId, name, description, content, defaultFlag) ->
    @RestService.post({
      path: jsRoutes.controllers.LandingPageService.updateLandingPage(pageId).url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag
      }
      authenticate: true
    })

  deleteLandingPage: (pageId) ->
    @RestService.delete
      path: jsRoutes.controllers.LandingPageService.deleteLandingPage(pageId).url

services.service('LandingPageService', LandingPageService)