class IndexController
	@$inject = [
		'$log', '$sce', '$scope', '$rootScope', '$location',
		'AuthProvider', 'DataService', 'AccountService', 
		'Events', 'Constants', 'SystemConfigurationService', 'LegalNoticeService', 'HtmlService', 'ErrorService'
	]
	constructor: (@$log, @$sce, @$scope, @$rootScope, @$location,
		@AuthProvider, @DataService, @AccountService, @Events, @Constants, @SystemConfigurationService, @LegalNoticeService, @HtmlService, @ErrorService) ->

		@$log.debug "Constructing MainController..."

		@isAuthenticated = @AuthProvider.isAuthenticated()
		@$log.debug "isAuthenticated: #{@isAuthenticated}"

		@theme = @SystemConfigurationService.getTheme()

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
			@LegalNoticeService.getDefaultLegalNotice()
			.then (data) =>
				html = @$sce.trustAsHtml(data.content) if data?
				@showLegalNotice(html)

	showLegalNotice: (html) ->
		@HtmlService.showHtml("Legal Notice", html)

controllers.controller('IndexController', IndexController)