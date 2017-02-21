class SystemConfigurationService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

  getSessionAliveTime: () ->
    @RestService.get({
      path: jsRoutes.controllers.SystemConfigurationService.getSessionAliveTime().url,
      authenticate: true
    })

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