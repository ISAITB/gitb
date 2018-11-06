class ErrorTemplateService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  getErrorTemplatesByCommunity: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.ErrorTemplateService.getErrorTemplatesByCommunity(communityId).url,
      authenticate: true
    })

  createErrorTemplate: (name, description, content, defaultFlag, communityId) ->
    @RestService.post({
      path: jsRoutes.controllers.ErrorTemplateService.createErrorTemplate().url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      }
      authenticate: true
    })

  getErrorTemplateById: (pageId) ->
    @RestService.get({
      path: jsRoutes.controllers.ErrorTemplateService.getErrorTemplateById(pageId).url,
      authenticate: true
    })

  updateErrorTemplate: (pageId, name, description, content, defaultFlag, communityId) ->
    @RestService.post({
      path: jsRoutes.controllers.ErrorTemplateService.updateErrorTemplate(pageId).url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      }
      authenticate: true
    })

  deleteErrorTemplate: (pageId) ->
    @RestService.delete
      path: jsRoutes.controllers.ErrorTemplateService.deleteErrorTemplate(pageId).url

  getCommunityDefaultErrorTemplate: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.ErrorTemplateService.getCommunityDefaultErrorTemplate().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })

services.service('ErrorTemplateService', ErrorTemplateService)