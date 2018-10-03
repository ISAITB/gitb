class OrganizationService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService']
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

  getOrganizationsByCommunity: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.getOrganizationsByCommunity(communityId).url,
      authenticate: true
    })

  createOrganization: (shortName, fullName, landingPage, legalNotice, otherOrganisation, communityId) ->
    data = {
      vendor_sname: shortName,
      vendor_fname: fullName,
      community_id: communityId,
    }

    if landingPage?
      data.landing_page_id = landingPage.id
    if legalNotice?
      data.legal_notice_id = legalNotice.id
    if otherOrganisation?
      data.other_organisation = otherOrganisation.id

    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.createOrganization().url,
      data: data
    })

  deleteOrganization: (orgId) ->
    @RestService.delete
      path: jsRoutes.controllers.OrganizationService.deleteOrganization(orgId).url

  updateOrganization: (orgId, shortName, fullName, landingPage, legalNotice, otherOrganisation) ->
    data = {
      vendor_sname: shortName,
      vendor_fname: fullName,
    }

    if landingPage?
      data.landing_page_id = landingPage.id
    if legalNotice?
      data.legal_notice_id = legalNotice.id
    if otherOrganisation?
      data.other_organisation = otherOrganisation.id

    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.updateOrganization(orgId).url,
      data: data,
      authenticate: true
    })

services.service('OrganizationService', OrganizationService)