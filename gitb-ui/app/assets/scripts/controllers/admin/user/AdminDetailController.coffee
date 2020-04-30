class AdminDetailController

  @$inject = ['$log', '$state', '$stateParams','UserManagementService', 'UserService', 'ValidationService', 'DataService', 'ConfirmationDialogService', 'ErrorService', 'PopupService']
  constructor: (@$log, @$state, @$stateParams, @UserManagementService, @UserService, @ValidationService, @DataService, @ConfirmationDialogService, @ErrorService, @PopupService) ->

    @alerts = []
    @userId = @$stateParams.id
    @user = {}

    if !@DataService.configuration['sso.enabled']
      @DataService.focus('name')

    @disableDeleteButton = Number(@DataService.user.id) == Number(@userId)

    # get selected user
    @UserService.getUserById(@userId)
    .then (data) =>
      @user = data
      @user.ssoStatusText = @DataService.userStatus(@user.ssoStatus)
      @UserManagementService.mapUser(@user)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  saveDisabled: () =>
    !(@user.name? && @user.name.trim() != '')

  # update and cancel detail
  updateAdmin: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
    (!@user.changePassword || @ValidationService.validatePasswords(@user.password, @user.cpassword, "Passwords do not match."))
      if @user.changePassword
        newPassword = @user.password
      @UserService.updateSystemAdminProfile(@userId, @user.name, newPassword)
      .then () =>
        @cancelDetailAdmin()
        @PopupService.success('Administrator updated')
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # delete and cancel detail
  deleteAdmin: () =>
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this administrator?", "Yes", "No")
    .then () =>
      @UserService.deleteAdmin(@userId)
      .then (data) =>
        @cancelDetailAdmin()
        @PopupService.success('Administrator deleted.')
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  # cancel detail
  cancelDetailAdmin: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'AdminDetailController', AdminDetailController
