class UserManagementController

  @$inject = ['$log', '$state', 'UserService', 'LandingPageService', 'OrganizationService', 'LegalNoticeService', 'ErrorService']
  constructor: (@$log, @$state, @UserService, @LandingPageService, @OrganizationService, @LegalNoticeService, @ErrorService) ->

    # admin table
    @adminColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'email',
        title: 'Email'
      }
    ]

    # organization table
    @organizationColums = [
      {
        field: 'sname',
        title: 'Short name'
      }
      {
        field: 'fname',
        title: 'Full name'
      }
    ]

    # landing page table
    @landingPagesColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
      {
        field: 'default',
        title: 'Default'
      }
    ]

    # legal notice table
    @legalNoticesColumns = [
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
      {
        field: 'default',
        title: 'Default'
      }
    ]

    @admins = []
    @organizations = []
    @landingPages = []
    @legalNotices = []

    # get all system administrators
    @UserService.getSystemAdministrators()
    .then (data) =>
      @admins = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get all organizations
    @OrganizationService.getOrganizations()
    .then (data) =>
      @organizations = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get all landing pages
    @LandingPageService.getLandingPages()
    .then (data) =>
      @landingPages = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    # get all legal notices
    @LegalNoticeService.getLegalNotices()
    .then (data) =>
      @legalNotices = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # detail of selected admin
  adminSelect: (admin) =>
    @$state.go 'app.admin.users.admins.detail', { id : admin.id }

  # detail of selected organization
  organizationSelect: (organization) =>
    @$state.go 'app.admin.users.organizations.detail.list', { id : organization.id }

  # detail of selected landing page
  landingPageSelect: (landingPage) =>
    @$state.go 'app.admin.users.landingpages.detail', { id : landingPage.id }

  # detail of selected legal notice
  legalNoticeSelect: (ln) =>
    @$state.go 'app.admin.users.legalnotices.detail', { id : ln.id }

@controllers.controller 'UserManagementController', UserManagementController
