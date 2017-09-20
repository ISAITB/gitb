class UserDetailController

  @$inject = ['$log', '$state', '$stateParams', 'ValidationService', 'UserManagementService', 'ConfirmationDialogService', 'UserService', 'Constants', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams,  @ValidationService, @UserManagementService, @ConfirmationDialogService, @UserService, @Constants, @ErrorService) ->

    @orgId = @$stateParams.org_id
    @userId = @$stateParams.user_id
    @alerts = []
    @user = {}

    @roleChoices = @Constants.VENDOR_USER_ROLES

    # get selected user
    @UserService.getUserById(@userId)
    .then (data) =>
      @user = data
      @UserManagementService.mapUser(@user)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  # update and cancel detail
  updateUser: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.")
      @UserService.updateUserProfile(@userId, @user.name, @user.role.id)
      .then (data) =>
        if (!data)
          @cancelDetailUser()
        else
          @ValidationService.pushAlert({type:'danger', msg:data.error_description})
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    @alerts = @ValidationService.getAlerts()

  # delete and cancel detail
  deleteUser: () =>
    @ValidationService.clearAll()
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this user?", "Yes", "No")
    .then () =>
      @UserService.deleteVendorUser(@userId)
      .then (data) =>
        if (!data)
          @cancelDetailUser()
        else
          @ValidationService.pushAlert({type:'danger', msg:data.error_description})
          @alerts = @ValidationService.getAlerts()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailUser: () =>
    @$state.go 'app.admin.users.communities.detail.organizations.detail.list', { org_id : @orgId }

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'UserDetailController', UserDetailController
