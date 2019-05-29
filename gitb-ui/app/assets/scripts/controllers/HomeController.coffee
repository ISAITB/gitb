class HomeController

  @$inject = ['$log', '$sce', 'Constants', 'AccountService', 'LandingPageService', 'ErrorService']
  constructor: (@$log, @$sce, @Constants, @AccountService, @LandingPageService, @ErrorService) ->

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
        .catch (error) =>
          @ErrorService.showErrorMessage(error)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

controllers.controller('HomeController', HomeController)