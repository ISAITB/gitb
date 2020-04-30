class OrganizationCreateController

  @$inject = ['$log', '$state', '$stateParams', 'LandingPageService', 'LegalNoticeService', 'ErrorTemplateService', 'ValidationService', 'OrganizationService', 'ErrorService', 'DataService', 'CommunityService', 'PopupService']
  constructor: (@$log, @$state, @$stateParams, @LandingPageService, @LegalNoticeService, @ErrorTemplateService, @ValidationService, @OrganizationService, @ErrorService, @DataService, @CommunityService, @PopupService) ->

    @communityId = @$stateParams.community_id

    @alerts = []
    @organization = {}
    @landingPages = []
    @legalNotices = []
    @errorTemplates = []
    @otherOrganisations = []
    @propertyData = {
      properties: []
      edit: false
    }

    @CommunityService.getOrganisationParameters(@communityId)
    .then (data) =>
      @propertyData.properties = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LandingPageService.getLandingPagesByCommunity(@communityId)
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LegalNoticeService.getLegalNoticesByCommunity(@communityId)
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ErrorTemplateService.getErrorTemplatesByCommunity(@communityId)
    .then (data) =>
      @errorTemplates = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @OrganizationService.getOrganizationsByCommunity(@communityId)
    .then (data) =>
      @otherOrganisations = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @DataService.focus('sname')

  valueDefined: (value) =>
    value? && value.trim().length > 0

  saveDisabled: () =>
    !(@valueDefined(@organization?.sname) && @valueDefined(@organization?.fname) && (!@DataService.configuration?['registration.enabled'] || (!@organization?.template || @valueDefined(@organization?.templateName))) && (!@propertyData.edit || @DataService.customPropertiesValid(@propertyData.properties)))

  copyChanged: () =>
    if @organization.otherOrganisations == undefined || @organization.otherOrganisations == null
      @organization.copyOrganisationParameters = false
      @organization.copySystemParameters = false
      @organization.copyStatementParameters = false
    else if @organization.copyOrganisationParameters
      @propertyData.edit = false

  # create organization and cancel screen
  createOrganization: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@organization.sname, "Please enter short name of the "+@DataService.labelOrganisationLower()+".") &
    @ValidationService.requireNonNull(@organization.fname, "Please enter full name of the "+@DataService.labelOrganisationLower()+".")
      @OrganizationService.createOrganization(@organization.sname, @organization.fname, @organization.landingPages, @organization.legalNotices, @organization.errorTemplates, @organization.otherOrganisations, @communityId, @organization.template, @organization.templateName, @propertyData.edit, @propertyData.properties, @organization.copyOrganisationParameters, @organization.copySystemParameters, @organization.copyStatementParameters)
      .then (data) =>
        if data? && data.error_code?
          @ValidationService.pushAlert({type:'danger', msg:data.error_description})
          @alerts = @ValidationService.getAlerts()          
        else
          @cancelCreateOrganization()
          @PopupService.success(@DataService.labelOrganisation()+" created.")
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create organization
  cancelCreateOrganization: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  # closes alert which is displayed due to an error
  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'OrganizationCreateController', OrganizationCreateController
