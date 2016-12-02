class HomeController

  @$inject = ['$log', '$sce', 'DataService', 'LandingPageService', 'ErrorService']
  constructor: (@$log, @$sce, @DataService, @LandingPageService, @ErrorService) ->

    @html = ""
    @vendor = @DataService.vendor

    if @vendor.landingPages?
      @html = @$sce.trustAsHtml(@DataService.vendor.landingPages.content)
    else
      @LandingPageService.getDefaultLandingPage()
      .then (data) =>
        @html = @$sce.trustAsHtml(data.content) if data?
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

controllers.controller('HomeController', HomeController)