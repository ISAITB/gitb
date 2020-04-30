class CommunityAdminCreateController

  @$inject = ['$log', '$state', '$stateParams', 'UserService', 'ValidationService', 'AuthService', 'ErrorService', 'DataService', 'PopupService']
  constructor: (@$log, @$state, @$stateParams, @UserService, @ValidationService, @AuthService, @ErrorService, @DataService, @PopupService) ->

    @communityId = @$stateParams.community_id

    @alerts = []
    @user = {}
    if @DataService.configuration['sso.enabled']
      @DataService.focus('email')
    else
      @DataService.focus('name')

  saveDisabled: () =>
    if @DataService.configuration['sso.enabled']
      !(@user.email? && @user.email.trim() != '')
    else
      !(@user.name? && @user.password? && @user.cpassword? && @user.email? && @user.name.trim() != '' && @user.password.trim() != '' && @user.cpassword.trim() != '' && @user.email.trim() != '')

  createAdmin: () =>
    @alerts = @ValidationService.clearAll()
    isSSO = @DataService.configuration['sso.enabled']
    if isSSO
      ok = @ValidationService.validateEmail(@user.email, "Please enter a valid email address.")
      emailCheckFunction = @AuthService.checkEmailOfCommunityAdmin
    else
      ok = @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
        @ValidationService.validatePasswords(@user.password, @user.cpassword, "Please enter equal passwords.") &
        @ValidationService.validateEmail(@user.email, "Please enter a valid email address.")
      emailCheckFunction = @AuthService.checkEmail

    if ok
      emailCheckFunction(@user.email, @communityId)
      .then (data) =>
        if (data.available)
          @UserService.createCommunityAdmin(@user.name, @user.email, @user.password, @communityId)
          .then () =>
           @cancelCreateAdmin()
           @PopupService.success('Administrator created.')
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          if isSSO
            @alerts.push({type:'danger', msg:"An administrator with email #{@user.email} has already been registered for this community."})
          else
            @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered."})
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  cancelCreateAdmin: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'CommunityAdminCreateController', CommunityAdminCreateController
