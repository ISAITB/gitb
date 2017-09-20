class LegalNoticeService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

  getLegalNoticesByCommunity: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.LegalNoticeService.getLegalNoticesByCommunity(communityId).url,
      authenticate: true
    })

  createLegalNotice: (name, description, content, defaultFlag, communityId) ->
    @RestService.post({
      path: jsRoutes.controllers.LegalNoticeService.createLegalNotice().url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      }
      authenticate: true
    })

  updateLegalNotice: (noticeId, name, description, content, defaultFlag, communityId) ->
    @RestService.post({
      path: jsRoutes.controllers.LegalNoticeService.updateLegalNotice(noticeId).url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag,
        community_id: communityId
      }
      authenticate: true
    })

  getLegalNoticeById: (noticeId) ->
    @RestService.get({
      path: jsRoutes.controllers.LegalNoticeService.getLegalNoticeById(noticeId).url,
      authenticate: true
    })

  deleteLegalNotice: (noticeId) ->
    @RestService.delete
      path: jsRoutes.controllers.LegalNoticeService.deleteLegalNotice(noticeId).url

  getCommunityDefaultLegalNotice: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.LegalNoticeService.getCommunityDefaultLegalNotice().url,
      authenticate: true,
      params: {
        community_id: communityId
      }
    })

services.service('LegalNoticeService', LegalNoticeService)