class OrganizationCreateController

  @$inject = ['$log', '$state', 'LandingPageService', 'LegalNoticeService', 'ValidationService', 'OrganizationService', 'ErrorService']
  constructor: (@$log, @$state, @LandingPageService, @LegalNoticeService, @ValidationService, @OrganizationService, @ErrorService) ->

    @alerts = []
    @organization = {}
    @landingPages = []
    @legalNotices = []

    @LandingPageService.getLandingPages()
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @LegalNoticeService.getLegalNotices()
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # create organization and cancel screen
  createOrganization: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@organization.sname, "Please enter short name of the organization.") &
    @ValidationService.requireNonNull(@organization.fname, "Please enter full name of the organization.")
      @OrganizationService.createOrganization @organization.sname, @organization.fname, @organization.landingPages, @organization.legalNotices
      .then () =>
        @cancelCreateOrganization()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create organization
  cancelCreateOrganization: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'OrganizationCreateController', OrganizationCreateController
