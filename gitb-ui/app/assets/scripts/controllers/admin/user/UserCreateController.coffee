class UserCreateController

  @$inject = ['$log', '$state', '$stateParams', 'ValidationService', 'UserService', 'Constants', 'AuthService', 'ErrorService', 'DataService', 'PopupService']
  constructor: (@$log, @$state, @$stateParams, @ValidationService, @UserService, @Constants, @AuthService, @ErrorService, @DataService, @PopupService) ->

    @orgId = @$stateParams.org_id
    @alerts = []
    @user = {}

    @roleCreateChoices = @Constants.VENDOR_USER_ROLES
    if @DataService.configuration['sso.enabled']
      @DataService.focus('email')
    else
      @DataService.focus('name')

  saveDisabled: () =>
    if @DataService.configuration['sso.enabled']
      !(@user.email? && @user.email.trim() != '' && @user.role?)
    else
      !(@user.name? && @user.password? && @user.cpassword? && @user.email? && @user.name.trim() != '' && @user.password.trim() != '' && @user.cpassword.trim() != '' && @user.email.trim() != '' && @user.role?)

  # create user and cancel screen
  createUser: () =>
    @alerts = @ValidationService.clearAll()
    isSSO = @DataService.configuration['sso.enabled']
    if isSSO
      ok = @ValidationService.validateEmail(@user.email, "Please enter a valid email address.") &
          @ValidationService.objectNonNull(@user.role, "Please enter a role.")
      emailCheckFunction = @AuthService.checkEmailOfOrganisationUser
    else
      ok = @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
        @ValidationService.objectNonNull(@user.role, "Please enter a role.") &
        @ValidationService.validatePasswords(@user.password, @user.cpassword, "Please enter equal passwords.") &
        @ValidationService.validateEmail(@user.email, "Please enter a valid email address.")
      emailCheckFunction = @AuthService.checkEmail

    if ok
      emailCheckFunction(@user.email, @orgId, @user.role.id)
      .then (data) =>
        if (data.available)
          @UserService.createVendorUser @user.name, @user.email, @user.password, @orgId, @user.role.id
          .then () =>
            @cancelCreateUser()
            @PopupService.success('User created.')
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          if isSSO
            @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered with the specified role for this organisation."})
          else
            @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered."})
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  # cancel create
  cancelCreateUser: () =>
    @$state.go 'app.admin.users.communities.detail.organizations.detail.list', { id : @orgId }

  # closes alert which is displayed due to an error
  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'UserCreateController', UserCreateController
