class UserService

  constructor: (@$log, @RestService) ->
    @$log.debug "Constructing UserService..."

  getSystemAdministrators: () ->
    @RestService.get({
      path: jsRoutes.controllers.UserService.getSystemAdministrators().url,
      authenticate: true
    })

  getUsersByOrganization: (orgId) ->
    @RestService.get({
      path: jsRoutes.controllers.UserService.getUsersByOrganization(orgId).url,
      authenticate: true
    })

services.service('UserService', UserService)