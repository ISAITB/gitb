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

  createOrganization: (shortName, fullName, landingPage, legalNotice) ->
    data = {
      vendor_sname: shortName,
      vendor_fname: fullName,
    }

    if landingPage?
      data.landing_page_id = landingPage.id
    if legalNotice?
      data.legal_notice_id = legalNotice.id

    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.createOrganization().url,
      data: data
    })

  deleteOrganization: (orgId) ->
    @RestService.delete
      path: jsRoutes.controllers.OrganizationService.deleteOrganization(orgId).url

  updateOrganization: (orgId, shortName, fullName, landingPage, legalNotice) ->
    data = {
      vendor_sname: shortName,
      vendor_fname: fullName,
    }

    if landingPage?
      data.landing_page_id = landingPage.id
    if legalNotice?
      data.legal_notice_id = legalNotice.id

    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.updateOrganization(orgId).url,
      data: data,
      authenticate: true
    })

services.service('OrganizationService', OrganizationService)