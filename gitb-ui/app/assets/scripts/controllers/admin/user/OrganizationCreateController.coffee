class OrganizationCreateController

  @$inject = ['$log', '$state', '$stateParams', 'LandingPageService', 'LegalNoticeService', 'ErrorTemplateService', 'ValidationService', 'OrganizationService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @LandingPageService, @LegalNoticeService, @ErrorTemplateService, @ValidationService, @OrganizationService, @ErrorService) ->

    @communityId = @$stateParams.community_id

    @alerts = []
    @organization = {}
    @landingPages = []
    @legalNotices = []
    @errorTemplates = []
    @otherOrganisations = []

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

  saveDisabled: () =>
    !(@organization?.sname? && @organization?.fname?)

  # create organization and cancel screen
  createOrganization: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@organization.sname, "Please enter short name of the organization.") &
    @ValidationService.requireNonNull(@organization.fname, "Please enter full name of the organization.")
      @OrganizationService.createOrganization @organization.sname, @organization.fname, @organization.landingPages, @organization.legalNotices, @organization.errorTemplates, @organization.otherOrganisations, @communityId
      .then () =>
        @cancelCreateOrganization()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create organization
  cancelCreateOrganization: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'OrganizationCreateController', OrganizationCreateController
