class CommunityCreateController

  @$inject = ['$log', '$state', 'ValidationService', 'CommunityService', 'ConformanceService', 'ErrorService']
  constructor: (@$log, @$state, @ValidationService, @CommunityService, @ConformanceService, @ErrorService) ->

    @alerts = []
    @community = {}
    @domains = []

    @ConformanceService.getDomains()
    .then (data) =>
      @domains = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  saveDisabled: () =>
    !(@community.sname? && @community.fname?)

  createCommunity: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@community.sname, "Please enter short name of the community.") &
    @ValidationService.requireNonNull(@community.fname, "Please enter full name of the community.") &
    (!(@community.email? && @community.email.trim() != '') || @ValidationService.validateEmail(@community.email, "Please enter a valid support email."))

      @CommunityService.createCommunity @community.sname, @community.fname, @community.email, @community.domain?.id
      .then () =>
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