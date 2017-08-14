class OrganizationDetailController

  @$inject = ['$log', '$state', '$stateParams', 'LandingPageService', 'LegalNoticeService', 'UserManagementService', 'ValidationService', 'ConfirmationDialogService', 'OrganizationService', 'UserService', 'Constants', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @LandingPageService, @LegalNoticeService, @UserManagementService, @ValidationService, @ConfirmationDialogService, @OrganizationService, @UserService, @Constants, @ErrorService) ->

    @userColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'email',
        title: 'Email'
      }
      {
        field: 'role',
        title: 'Role'
      }
    ]

    @orgId = @$stateParams.id
    @organization = {}
    @landingPages = []
    @legalNotices = []
    @users = []
    @alerts = []

    # get selected organization
    @OrganizationService.getOrganizationById(@orgId)
    .then (data) =>
      @organization = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get users of the organization
    @UserService.getUsersByOrganization(@orgId)
    .then (data) =>
      @users = data
      @UserManagementService.mapUsers(@users)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

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

  # delete and cancel detail
  deleteOrganization: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this organization?", "Yes", "No")
    .then () =>
      @OrganizationService.deleteOrganization(@orgId)
      .then () =>
        @cancelDetailOrganization()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateOrganization: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@organization.sname, "Please enter short name of the organization.") &
    @ValidationService.requireNonNull(@organization.fname, "Please enter full name of the organization.")
      @OrganizationService.updateOrganization(@orgId, @organization.sname, @organization.fname, @organization.landingPages, @organization.legalNotices)
      .then () =>
        @cancelDetailOrganization()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # detail of selected organization
  userSelect: (user) =>
    @$state.go 'app.admin.users.organizations.detail.users.detail.list', { user_id : user.id }

  # cancel detail
  cancelDetailOrganization: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'OrganizationDetailController', OrganizationDetailController
