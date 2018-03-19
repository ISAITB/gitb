class IndexController
	@$inject = [
		'$log', '$sce', '$scope', '$rootScope', '$location', '$state', '$window'
		'AuthProvider', 'SystemConfigurationService', 'DataService', 'AccountService',
		'Events', 'Constants', 'LegalNoticeService', 'HtmlService', 'ErrorService'
	]
	constructor: (@$log, @$sce, @$scope, @$rootScope, @$location, @$state, @$window,
		@AuthProvider, @SystemConfigurationService, @DataService, @AccountService, @Events, @Constants,@LegalNoticeService, @HtmlService, @ErrorService) ->

		@$log.debug "Constructing MainController..."

		@isAuthenticated = @AuthProvider.isAuthenticated()
		@$log.debug "isAuthenticated: #{@isAuthenticated}"

		@logo
		@footer
		@version = @Constants.VERSION

		@SystemConfigurationService.getLogo()
		.then (data) =>
			@logo = data
		.catch (error) =>
			@logo = @Constants.DEFAULT_LOGO

		@SystemConfigurationService.getFooterLogo()
		.then (data) =>
			@footer = data

		if @isAuthenticated
			@getUserProfile()
			@getVendorProfile()

		#register for login events
		@$rootScope.$on @Events.afterLogin, (event, params) =>
			@$log.debug "handling after-login"
			@isAuthenticated = true
			@getUserProfile()
			@getVendorProfile()
			@redirect('/')

	getUserProfile : () ->
		if !@DataService.user?
			@AccountService.getUserProfile()
			.then(
				(data) =>
					@DataService.setUser(data)
					@$log.debug angular.toJson(data)
				,
				(error) =>
					@ErrorService.showErrorMessage(error)
			)

	getVendorProfile: () ->
		@AccountService.getVendorProfile()
		.then(
			(data) =>
				@DataService.setVendor(data)
			,
			(error) =>
				@ErrorService.showErrorMessage(error)
		)

	redirect: (address) ->
		@$location.path(address)

	logout: () ->
		@$rootScope.$emit(@Events.onLogout)
		@DataService.destroy()
		@isAuthenticated = false
		@redirect('/login')

	onLegalNotice: () ->
		vendor = @DataService.vendor
		if vendor? && vendor.legalNotices?
			html = @$sce.trustAsHtml(vendor.legalNotices.content)
			@showLegalNotice(html)
		else
		    @LegalNoticeService.getCommunityDefaultLegalNotice(vendor.community)
            .then (data) =>
              if data.exists == true
                html = @$sce.trustAsHtml(data.content)
                @showLegalNotice(html)
              else
                if vendor.community != @Constants.DEFAULT_COMMUNITY_ID
                  @LegalNoticeService.getCommunityDefaultLegalNotice(@Constants.DEFAULT_COMMUNITY_ID)
                  .then (data) =>
                    if data.exists == true
                      html = @$sce.trustAsHtml(data.content)
                      @showLegalNotice(html)
                  .catch (error) =>
                    @ErrorService.showErrorMessage(error)
            .catch (error) =>
              @ErrorService.showErrorMessage(error)

	showLegalNotice: (html) ->
		@HtmlService.showHtml("Legal Notice", html)

	onTestsClick: () ->
		@$window.localStorage['organization'] = angular.toJson @DataService.vendor
		@$window.localStorage['community'] = angular.toJson @DataService.community
		@$state.go 'app.systems.list'

controllers.controller('IndexController', IndexController)