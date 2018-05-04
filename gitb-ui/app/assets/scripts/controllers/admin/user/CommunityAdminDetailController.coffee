class CommunityAdminDetailController

  @$inject = ['$log', '$state', '$stateParams','UserManagementService', 'UserService', 'ValidationService', 'DataService', 'ConfirmationDialogService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @UserManagementService, @UserService, @ValidationService, @DataService, @ConfirmationDialogService, @ErrorService) ->

    @communityId = @$stateParams.community_id
    @userId = @$stateParams.admin_id
    @alerts = []
    @user = {}

    @disableDeleteButton = Number(@DataService.user.id) == Number(@userId)

    @UserService.getUserById(@userId)
    .then (data) =>
      @user = data
      @UserManagementService.mapUser(@user)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  saveDisabled: () =>
    !(@user.name?)

  updateAdmin: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.")
      @UserService.updateCommunityAdminProfile(@userId, @user.name)
      .then () =>
        @cancelDetailAdmin()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  deleteAdmin: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this administrator?", "Yes", "No")
    .then () =>
      @UserService.deleteAdmin(@userId)
      .then (data) =>
        @cancelDetailAdmin()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  cancelDetailAdmin: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'CommunityAdminDetailController', CommunityAdminDetailController
