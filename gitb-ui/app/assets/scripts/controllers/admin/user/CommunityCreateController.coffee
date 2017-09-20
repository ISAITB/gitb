class CommunityCreateController

  @$inject = ['$log', '$state', 'ValidationService', 'LandingPageService', 'LegalNoticeService', 'CommunityService', 'ConformanceService', 'ErrorService']
  constructor: (@$log, @$state, @ValidationService, @LandingPageService, @LegalNoticeService, @CommunityService, @ConformanceService, @ErrorService) ->

    @alerts = []
    @community = {}
    @domains = []

    @ConformanceService.getDomains()
    .then (data) =>
      @domains = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  createCommunity: () =>
    @ValidationService.clearAll()
    if @ValidationService.requireNonNull(@community.sname, "Please enter short name of the community.") &
    @ValidationService.requireNonNull(@community.fname, "Please enter full name of the community.")
      @CommunityService.createCommunity @community.sname, @community.fname, @community.domain?.id
      .then () =>
        @cancelCreateCommunity()
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
    else
      @alerts = @ValidationService.getAlerts()

  cancelCreateCommunity: () =>
    @$state.go 'app.admin.users.list'

  closeAlert: (index) ->
    @ValidationService.clearAlert(index)

@controllers.controller 'CommunityCreateController', CommunityCreateController