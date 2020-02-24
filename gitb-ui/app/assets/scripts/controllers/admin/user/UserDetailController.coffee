class UserDetailController

  @$inject = ['$log', '$state', '$stateParams', 'ValidationService', 'UserManagementService', 'ConfirmationDialogService', 'UserService', 'Constants', 'ErrorService', 'DataService', 'AuthService', '$q', 'PopupService']
  constructor: (@$log, @$state, @$stateParams,  @ValidationService, @UserManagementService, @ConfirmationDialogService, @UserService, @Constants, @ErrorService, @DataService, @AuthService, @$q, @PopupService) ->

    @orgId = @$stateParams.org_id
    @userId = @$stateParams.user_id
    @alerts = []
    @user = {}

    if @DataService.configuration['sso.enabled']
      @DataService.focus('role')
    else
      @DataService.focus('name')

    @roleChoices = @Constants.VENDOR_USER_ROLES

    # get selected user
    @UserService.getUserById(@userId)
    .then (data) =>
      @user = data
      @user.ssoStatusText = @DataService.userStatus(@user.ssoStatus)
      @UserManagementService.mapUser(@user)
      @originalRoleId = @user.role.id
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  saveDisabled: () =>
    if @DataService.configuration['sso.enabled']
      !(@user.role?)
    else
      !(@user.name? && @user.role?)

  # update and cancel detail
  updateUser: () =>
    @alerts = @ValidationService.clearAll()
    @okToUpdate = false
    @updateDeferred = @$q.defer()
    isSSO = @DataService.configuration['sso.enabled']
    if isSSO
      if @originalRoleId != @user.role.id
        @AuthService.checkEmailOfOrganisationUser(@user.email, @orgId, @user.role.id)
        .then (data) =>
          if data.available
            @okToUpdate = true
          else
            @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered with the specified role for this organisation."})
            @okToUpdate = false
          @updateDeferred.resolve()
      else
        @okToUpdate = true
        @updateDeferred.resolve()
    else
      @okToUpdate = @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
        (!@user.changePassword || @ValidationService.validatePasswords(@user.password, @user.cpassword, "Passwords do not match."))
      @alerts = @ValidationService.getAlerts()
      @updateDeferred.resolve()

    @$q.all([@updateDeferred.promise]).then(() =>
      if @okToUpdate
        if @user.changePassword
          newPassword = @user.password
        @UserService.updateUserProfile(@userId, @user.name, @user.role.id, newPassword)
        .then (data) =>
          if (!data)
            @cancelDetailUser()
            @PopupService.success('User updated.')
          else
            @ValidationService.pushAlert({type:'danger', msg:data.error_description})
        .catch (error) =>
          @ErrorService.showErrorMessage(error)
    )
  
  # delete and cancel detail
  deleteUser: () =>
    @ValidationService.clearAll()
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this user?", "Yes", "No")
    .then () =>
      @UserService.deleteVendorUser(@userId)
      .then (data) =>
        if (!data)
          @cancelDetailUser()
          @PopupService.success('User deleted.')
        else
          @ValidationService.pushAlert({type:'danger', msg:data.error_description})
          @alerts = @ValidationService.getAlerts()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailUser: () =>
    @$state.go 'app.admin.users.communities.detail.organizations.detail.list', { org_id : @orgId }

  # closes alert which is displayed due to an error
  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'UserDetailController', UserDetailController
