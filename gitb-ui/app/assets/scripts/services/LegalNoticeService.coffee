class LegalNoticeService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

  getLegalNotices: () ->
    @RestService.get({
      path: jsRoutes.controllers.LegalNoticeService.getLegalNotices().url,
      authenticate: true
    })

  getLegalNoticeById: (noticeId) ->
    @RestService.get({
      path: jsRoutes.controllers.LegalNoticeService.getLegalNoticeById(noticeId).url,
      authenticate: true
    })

  getDefaultLegalNotice: () ->
    @RestService.get({
      path: jsRoutes.controllers.LegalNoticeService.getDefaultLegalNotice().url,
      authenticate: true
    })

  createLegalNotice: (name, description, content, defaultFlag) ->
    @RestService.post({
      path: jsRoutes.controllers.LegalNoticeService.createLegalNotice().url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag
      }
      authenticate: true
    })

  updateLegalNotice: (noticeId, name, description, content, defaultFlag) ->
    @RestService.post({
      path: jsRoutes.controllers.LegalNoticeService.updateLegalNotice(noticeId).url,
      data: {
        name: name,
        description: description,
        content: content,
        default_flag: defaultFlag
      }
      authenticate: true
    })

  deleteLegalNotice: (noticeId) ->
    @RestService.delete
      path: jsRoutes.controllers.LegalNoticeService.deleteLegalNotice(noticeId).url

services.service('LegalNoticeService', LegalNoticeService)