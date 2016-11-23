class OrganizationService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  constructor: (@$log, @RestService) ->

  getOrganizations: () ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.getOrganizations().url,
      authenticate: true
    })

  getOrganizationById: (orgId) ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.getOrganizationById(orgId).url,
      authenticate: true
    })

  createOrganization: (shortName, fullName) ->
    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.createOrganization().url,
      data: {
        vendor_sname : shortName,
        vendor_fname : fullName
      }
    })

  deleteOrganization: (orgId) ->
    @RestService.delete
      path: jsRoutes.controllers.OrganizationService.deleteOrganization(orgId).url

  updateOrganization: (orgId, shortName, fullName) ->
    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.updateOrganization(orgId, shortName, fullName).url,
      authenticate: true
    })

services.service('OrganizationService', OrganizationService)