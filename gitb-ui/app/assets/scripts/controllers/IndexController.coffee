class IndexController
	@$inject = [
		'$log', '$sce', '$scope', '$rootScope', '$location', '$state', '$window'
		'AuthProvider', 'SystemConfigurationService', 'DataService', 'AccountService',
		'Events', 'Constants', 'LegalNoticeService', 'HtmlService', 'ErrorService', '$uibModal', '$q', 'ConfirmationDialogService'
	]
	constructor: (@$log, @$sce, @$scope, @$rootScope, @$location, @$state, @$window,
		@AuthProvider, @SystemConfigurationService, @DataService, @AccountService, @Events, @Constants,@LegalNoticeService, @HtmlService, @ErrorService, @$uibModal, @$q, @ConfirmationDialogService) ->

		@$log.debug "Constructing MainController..."

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
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		if @AuthProvider.isAuthenticated()
			@getUserProfile()
			@getVendorProfile()
			@getConfigurationProperties()
			if @DataService.user? && @DataService.user.onetime
				@redirect('/onetime')

		#register for login events
		@$rootScope.$on @Events.afterLogin, (event, params) =>
			@$log.debug "handling after-login"
			@profileLoaded = @$q.defer()
			@getUserProfile()
			@$q.all([@profileLoaded.promise]).then(() =>
				if @DataService.user.onetime
					# One time password - force replace
					@redirect('/onetime')
				else
					@getVendorProfile()
					@getConfigurationProperties()
					@redirect('/')
			)

	getConfigurationProperties : () ->
		if !@DataService.configuration?
			@AccountService.getConfiguration()
			.then(
				(data) =>
					@DataService.setConfiguration(data)
				,
				(error) =>
					@ErrorService.showErrorMessage(error)
			)

	getUserProfile : () ->
		if !@DataService.user?
			@AccountService.getUserProfile()
			.then(
				(data) =>
					@DataService.setUser(data)
					@profileLoaded.resolve()
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

	onLegalNotice: () =>
		vendor = @DataService.vendor
		if vendor? && vendor.legalNotices?
			html = @$sce.trustAsHtml(vendor.legalNotices.content)
			@showLegalNotice(html)
		else
			if vendor?
				communityId = vendor.community
				response = @LegalNoticeService.getCommunityDefaultLegalNotice(communityId)
			else 
				response = @LegalNoticeService.getTestBedDefaultLegalNotice()
			response.then (data) =>
				if data.exists == true
					html = @$sce.trustAsHtml(data.content)
					@showLegalNotice(html)
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	showLegalNotice: (html) ->
		@HtmlService.showHtml("Legal Notice", html)

	showContactUs: () ->
		@DataService.configuration?["email.enabled"] == 'true'

	contactUs: () =>
		modalOptions =
			templateUrl: 'assets/views/components/contact-support.html'
			controller: 'ContactSupportController as controller'
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions).result.finally(angular.noop).then(angular.noop, angular.noop)

	showProvideFeedback: () =>
		!@showContactUs() && (@DataService.configuration?["survey.enabled"] == 'true') && !@DataService.user.onetime

	provideFeedbackLink: () =>
		@DataService.configuration?["survey.address"]

	userGuideLink: () =>
		if (@DataService.configuration?)
			if @DataService.isVendorAdmin
				@DataService.configuration['userguide.oa']
			else if @DataService.isCommunityAdmin
				@DataService.configuration['userguide.ca']
			else if @DataService.isSystemAdmin
				@DataService.configuration['userguide.ta']
			else
				@DataService.configuration['userguide.ou']

	showUserGuide: () =>
		@DataService.user? && !@DataService.user.onetime

	onTestsClick: () ->
		@$window.localStorage['organization'] = angular.toJson @DataService.vendor
		@$window.localStorage['community'] = angular.toJson @DataService.community
		@$state.go 'app.systems.list'

	toAdmin:() =>
		@DataService.clearSearchState()

controllers.controller('IndexController', IndexController)