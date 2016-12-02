class IndexController
	@$inject = [
		'$log', '$scope', '$rootScope', '$location', 
		'AuthProvider', 'DataService', 'AccountService', 
		'Events', 'Constants', 'ErrorService'
	]
	constructor: (@$log, @$scope, @$rootScope, @$location,
		@AuthProvider, @DataService, @AccountService, @Events, @Constants, @ErrorService) ->

		@$log.debug "Constructing MainController..."

		@isAuthenticated = @AuthProvider.isAuthenticated()
		@$log.debug "isAuthenticated: #{@isAuthenticated}"

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

controllers.controller('IndexController', IndexController)