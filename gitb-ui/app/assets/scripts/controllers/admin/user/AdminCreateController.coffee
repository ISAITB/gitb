class AdminCreateController

  @$inject = ['$log', '$state', 'UserService', 'ValidationService', 'AuthService', 'Constants', 'ErrorService']
  constructor: (@$log, @$state, @UserService, @ValidationService, @AuthService, @Constants, @ErrorService) ->

    @alerts = []
    @user = {}

  saveDisabled: () =>
    !(@user.name? && @user.password? && @user.cpassword? && @user.email?)

  # create system administrator and cancel screen
  createAdmin: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
    @ValidationService.validatePasswords(@user.password, @user.cpassword, "Please enter equal passwords.") &
    @ValidationService.validateEmail(@user.email, "Please enter a valid email address.")
      @AuthService.checkEmail(@user.email)
      .then (data) =>
        if (data.available)
          @UserService.createSystemAdmin(@user.name, @user.email, @user.password, @Constants.DEFAULT_COMMUNITY_ID)
          .then () =>
           @cancelCreateAdmin()
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered."})
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create
  cancelCreateAdmin: () =>
    @$state.go 'app.admin.users.list'

  # closes alert which is displayed due to an error
  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'AdminCreateController', AdminCreateController
