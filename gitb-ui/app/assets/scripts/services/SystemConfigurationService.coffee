class SystemConfigurationService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

    @theme

  getSessionAliveTime: () ->
    @RestService.get({
      path: jsRoutes.controllers.SystemConfigurationService.getSessionAliveTime().url,
      authenticate: true
    })

  getTheme: () ->
    @theme

  resolveTheme: () ->
    @RestService.get({
      path: jsRoutes.controllers.SystemConfigurationService.getTheme().url,
      authenticate: false
    })
    .then (data) =>
      @theme = data.theme

  updateSessionAliveTime: (value) ->
    data = {}
    if value?
      data.parameter = value

    @RestService.post({
      path: jsRoutes.controllers.SystemConfigurationService.updateSessionAliveTime().url,
      data: data
      authenticate: true
    })

services.service('SystemConfigurationService', SystemConfigurationService)