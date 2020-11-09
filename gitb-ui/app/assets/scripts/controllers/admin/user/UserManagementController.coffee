class UserManagementController

  @$inject = ['$log', '$state', 'DataService', 'UserService', 'OrganizationService', 'CommunityService', 'ErrorService', 'Constants']
  constructor: (@$log, @$state, @DataService, @UserService, @OrganizationService, @CommunityService, @ErrorService, @Constants) ->

    @adminStatus = {status: @Constants.STATUS.PENDING}
    @communityStatus = {status: @Constants.STATUS.PENDING}

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
      @adminStatus.status = @Constants.STATUS.FINISHED
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @adminStatus.status = @Constants.STATUS.FINISHED

    @CommunityService.getCommunities()
    .then (data) =>
      @communities = data
      @communityStatus.status = @Constants.STATUS.FINISHED
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
      @communityStatus.status = @Constants.STATUS.FINISHED

  userStatus: (ssoStatus) =>
      @DataService.userStatus(ssoStatus)

  # detail of selected admin
  adminSelect: (admin) =>
    @$state.go 'app.admin.users.admins.detail', { id : admin.id }

  communitySelect: (community) =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : community.id }

@controllers.controller 'UserManagementController', UserManagementController