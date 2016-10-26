class OrganizationService

  constructor: (@$log, @RestService) ->
    @$log.debug "Constructing OrganizationService..."

  getOrganizations: () ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.getOrganizations().url,
      authenticate: true
    })

services.service('OrganizationService', OrganizationService)