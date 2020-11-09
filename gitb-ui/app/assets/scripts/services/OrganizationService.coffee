class OrganizationService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = { headers: @headers }

  @$inject = ['$log', 'RestService', 'DataService']
  constructor: (@$log, @RestService, @DataService) ->

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

  getOrganizationBySystemId: (systemId) ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.getOrganizationBySystemId(systemId).url,
      authenticate: true
    })

  getOrganizationsByCommunity: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.getOrganizationsByCommunity(communityId).url,
      authenticate: true
    })

  createOrganization: (shortName, fullName, landingPage, legalNotice, errorTemplate, otherOrganisation, communityId, template, templateName, processProperties, properties, copyOrganisationParameters, copySystemParameters, copyStatementParameters) ->
    data = {
      vendor_sname: shortName,
      vendor_fname: fullName,
      community_id: communityId
    }
    if @DataService.configuration['registration.enabled']
      data.template =  template
      if template && templateName?
        data.template_name = templateName

    if landingPage?
      data.landing_page_id = landingPage.id
    if legalNotice?
      data.legal_notice_id = legalNotice.id
    if errorTemplate?
      data.error_template_id = errorTemplate.id
    if otherOrganisation?
      data.other_organisation = otherOrganisation.id
      data.org_params = copyOrganisationParameters
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters

    if processProperties
      data.properties = @DataService.customPropertiesForPost(properties)

    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.createOrganization().url,
      data: data
    })

  deleteOrganization: (orgId) ->
    @RestService.delete
      path: jsRoutes.controllers.OrganizationService.deleteOrganization(orgId).url

  updateOrganization: (orgId, shortName, fullName, landingPage, legalNotice, errorTemplate, otherOrganisation, template, templateName, processProperties, properties, copyOrganisationParameters, copySystemParameters, copyStatementParameters) ->
    data = {
      vendor_sname: shortName,
      vendor_fname: fullName
    }
    if @DataService.configuration['registration.enabled']
      data.template =  template
      if template && templateName?
        data.template_name = templateName

    if landingPage?
      data.landing_page_id = landingPage.id
    if legalNotice?
      data.legal_notice_id = legalNotice.id
    if errorTemplate?
      data.error_template_id = errorTemplate.id
    if otherOrganisation?
      data.other_organisation = otherOrganisation.id
      data.org_params = copyOrganisationParameters
      data.sys_params = copySystemParameters
      data.stm_params = copyStatementParameters
    if processProperties
      data.properties = @DataService.customPropertiesForPost(properties)

    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.updateOrganization(orgId).url,
      data: data,
      authenticate: true
    })
  
  getOwnOrganisationParameterValues: () ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.getOwnOrganisationParameterValues().url,
      authenticate: true
    })

  checkOrganisationParameterValues: (orgId) ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.checkOrganisationParameterValues(orgId).url,
      authenticate: true
    })

  getOrganisationParameterValues: (orgId) ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.getOrganisationParameterValues(orgId).url,
      authenticate: true
    })

  updateOrganisationParameterValues: (orgId, values) ->
    data = {
      values: values
    }
    @RestService.post({
      path: jsRoutes.controllers.OrganizationService.updateOrganisationParameterValues(orgId).url,
      data: data,
      authenticate: true
    })

  ownOrganisationHasTests: () ->
    @RestService.get({
      path: jsRoutes.controllers.OrganizationService.ownOrganisationHasTests().url,
      authenticate: true
    })

services.service('OrganizationService', OrganizationService)