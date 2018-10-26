class CommunityAdminCreateController

  @$inject = ['$log', '$state', '$stateParams', 'UserService', 'ValidationService', 'AuthService', 'ErrorService']
  constructor: (@$log, @$state, @$stateParams, @UserService, @ValidationService, @AuthService, @ErrorService) ->

    @communityId = @$stateParams.community_id

    @alerts = []
    @user = {}

  saveDisabled: () =>
    !(@user.name? && @user.password? && @user.cpassword? && @user.email?)

  createAdmin: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@user.name, "Please enter a name.") &
    @ValidationService.validatePasswords(@user.password, @user.cpassword, "Please enter equal passwords.") &
    @ValidationService.validateEmail(@user.email, "Please enter a valid email address.")
      @AuthService.checkEmail(@user.email)
      .then (data) =>
        if (data.available)
          @UserService.createCommunityAdmin(@user.name, @user.email, @user.password, @communityId)
          .then () =>
           @cancelCreateAdmin()
          .catch (error) =>
            @ErrorService.showErrorMessage(error)
        else
          @alerts.push({type:'danger', msg:"A user with email #{@user.email} has already been registered."})
    else
      @alerts = @ValidationService.getAlerts()

  cancelCreateAdmin: () =>
    @$state.go 'app.admin.users.communities.detail.list', { community_id : @communityId }

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'CommunityAdminCreateController', CommunityAdminCreateController
