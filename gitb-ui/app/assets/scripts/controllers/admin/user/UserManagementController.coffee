class UserManagementController

  @$inject = ['$log', '$state', 'DataService', 'UserService', 'OrganizationService', 'CommunityService', 'ErrorService']
  constructor: (@$log, @$state, @DataService, @UserService, @OrganizationService, @CommunityService, @ErrorService) ->

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
      {
        field: 'ssoStatusText',
        title: 'Status'
      }
    ]

    @communityColumns = [
      {
        field: 'sname',
        title: 'Short name'
      }
      {
        field: 'fname',
        title: 'Full name'
      }
    ]

    @admins = []
    @communities = []

    if !@DataService.isSystemAdmin
      @$state.go 'app.home'

    @UserService.getSystemAdministrators()
    .then (data) =>
      for admin in data
        admin.ssoStatusText = @userStatus(admin.ssoStatus)
      @admins = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @CommunityService.getCommunities()
    .then (data) =>
      @communities = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  userStatus: (ssoStatus) =>
      @DataService.userStatus(ssoStatus)

  # detail of selected admin
  adminSelect: (admin) =>
    @$state.go 'app.admin.users.admins.detail', { id : admin.id }

  communitySelect: (community) =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : community.id }

@controllers.controller 'UserManagementController', UserManagementController