class HomeController

  @$inject = ['$log', '$sce', 'Constants', 'AccountService', 'DataService', 'LandingPageService', 'ErrorService']
  constructor: (@$log, @$sce, @Constants, @AccountService, @DataService, @LandingPageService, @ErrorService) ->

    @html = ""

    @AccountService.getVendorProfile()
    .then (vendor) =>
      if vendor.landingPages?
        @html = @$sce.trustAsHtml(vendor.landingPages.content)
      else
        communityId = vendor.community
        @LandingPageService.getCommunityDefaultLandingPage(communityId)
        .then (data) =>
          if data.exists == true
            @html = @$sce.trustAsHtml(data.content)
          else if communityId != @Constants.DEFAULT_COMMUNITY_ID
            @LandingPageService.getCommunityDefaultLandingPage(@Constants.DEFAULT_COMMUNITY_ID)
            .then (data) =>
              @html = @$sce.trustAsHtml(data.content) if data.exists == true
            .catch (error) =>
              @ErrorService.showErrorMessage(error)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

controllers.controller('HomeController', HomeController)