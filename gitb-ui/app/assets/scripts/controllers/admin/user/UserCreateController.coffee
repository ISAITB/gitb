class UserCreateController

  @$inject = ['$log', '$state', '$stateParams', 'ValidationService', 'UserService', 'Constants', 'AuthService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @ValidationService, @UserService, @Constants, @AuthService, @ErrorService) ->

    @orgId = @$stateParams.id
    @alerts = []
    @user = {}

    @roleCreateChoices = @Constants.VENDOR_USER_ROLES

  # create user and cancel screen
  createUser: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
    @ValidationService.objectNonNull(@user.role, "Please enter a role.") &
    @ValidationService.validatePasswords(@user.password, @user.cpassword, "Please enter equal passwords.") &
    @ValidationService.validateEmail(@user.email, "Please enter a valid email address.")
      @AuthService.checkEmail(@user.email)
      .then (data) =>
        if (data.available)
          @UserService.createVendorUser @user.name, @user.email, @user.password, @orgId, @user.role.id
          .then () =>
            @cancelCreateUser()
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered."})
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create
  cancelCreateUser: () =>
    @$state.go 'app.admin.users.organizations.detail.list', { id : @orgId }

  # closes alert which is displayed due to an error
  closeAlert: (index) ->
    @ValidationService.clearAlert(index)


@controllers.controller 'UserCreateController', UserCreateController