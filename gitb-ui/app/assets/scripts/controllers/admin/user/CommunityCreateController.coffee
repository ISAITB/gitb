class CommunityCreateController

  @$inject = ['$log', '$state', 'ValidationService', 'CommunityService', 'ConformanceService', 'ErrorService', 'Constants', 'DataService']
  constructor: (@$log, @$state, @ValidationService, @CommunityService, @ConformanceService, @ErrorService, @Constants, @DataService) ->

    @alerts = []
    @community = {}
    @domains = []

    @ConformanceService.getDomains()
    .then (data) =>
      @domains = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  saveDisabled: () =>
    !(@community?.sname? && @community?.fname? && (!@DataService.configuration?['registration.enabled'] || 
    (@community?.selfRegType == @Constants.SELF_REGISTRATION_TYPE.NOT_SUPPORTED || @community?.selfRegType == @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING || (@community?.selfRegToken?.trim().length > 0))))

  createCommunity: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@community.sname, "Please enter short name of the community.") &
    @ValidationService.requireNonNull(@community.fname, "Please enter full name of the community.") &
    (!(@community.email? && @community.email.trim() != '') || @ValidationService.validateEmail(@community.email, "Please enter a valid support email."))

      @CommunityService.createCommunity @community.sname, @community.fname, @community.email, @community.selfRegType, @community.selfRegToken, @community.domain?.id
      .then (data) =>
        if data? && data.error_code?
          @ValidationService.pushAlert({type:'danger', msg:data.error_description})
          @alerts = @ValidationService.getAlerts()          
        else
          @cancelCreateCommunity()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  cancelCreateCommunity: () =>
    @$state.go 'app.admin.users.list'

  closeAlert: (index) =>
    @ValidationService.clearAlert(index)
    @alerts = @ValidationService.getAlerts()

@controllers.controller 'CommunityCreateController', CommunityCreateController