class LoginController

	@$inject = [
		'$log', '$rootScope', '$location', '$http', '$uibModal'
		'AuthService', 'AuthProvider', 'Events', 'Constants', 'ErrorService', 'RestService', 'DataService', '$cookies', '$window'
	]
	constructor: (@$log, @$rootScope, @$location, @$http, @$uibModal, @AuthService,
		@AuthProvider, @Events, @Constants, @ErrorService, @RestService, @DataService, @$cookies, @$window) ->
		@$log.debug "Constructing LoginController..."
		if (@AuthProvider.isAuthenticated())
			@$location.path('/')

		@loginOption = @$cookies.get(@Constants.LOGIN_OPTION_COOKIE_KEY)
		if !@loginOption?
			@loginOption == @Constants.LOGIN_OPTION.NONE
		@alerts = []	  # alerts to be displayed
		@spinner = false # spinner to be display while waiting response from the server
		@createPending = false

		if @loginOption == @Constants.LOGIN_OPTION.REGISTER || (@DataService.configuration['sso.inMigration'] && @loginOption == @Constants.LOGIN_OPTION.MIGRATE)
			@createAccount(@loginOption)
		else if @loginOption == @Constants.LOGIN_OPTION.DEMO
			@loginViaSelection(@DataService.configuration['demos.account'])

	createAccount:(loginOption) ->
		if !loginOption?
			loginOption = @Constants.LOGIN_OPTION.NONE
		@createPending = true
		modalOptions =
			templateUrl: 'assets/views/components/link-account-modal.html'
			controller: 'LinkAccountModalController as controller'
			size: 'lg'
			resolve:
				linkedAccounts: () => @AuthService.getUserUnlinkedFunctionalAccounts()
				createOption: () => loginOption
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result.finally(angular.noop).then(
			() => @createPending = false, 
			() => @createPending = false
		)

	loginViaSelection: (userId) ->
		data = {
			id: userId
		}
		options = @RestService.configureOptions(
			'POST', 
			jsRoutes.controllers.AuthenticationService.selectFunctionalAccount().url.substring(1),
			undefined,
			data,
			false,
			undefined
		)
		@loginInternal(options)

	loginInternal: (options) ->
		@spinner = true #start spinner before calling service operation
		@$http(options).then(
			(result) =>
				# login successful, fire onLogin event so that our authentication provider
				# authenticaes user to the system
				path = '/'
				if result.headers('ITB-PATH')
					path = result.headers('ITB-PATH')
				else if result.data.path?
					path = result.data.path
				@$rootScope.$emit(@Events.onLogin, {
					tokens: result.data,
					path: path,
					remember: if @rememberme? then @rememberme else false
				})
				@spinner = false #stop spinner
			(error) =>
				switch error.status
					when 401  # Unauthorized
						@alerts.push({type:'danger', msg:"Incorrect email or password."})
					else
						@ErrorService.showErrorMessage(error)
				@password = '' #clear password field
				@spinner = false		 #stop spinner
		)

	cancelLogin: () ->
		url = @$location.absUrl()
		@$window.location.href = url.substring(0, url.indexOf('app#!'))

	loginDisabled: () ->
		@spinner || @email == undefined || @email == '' || @password == undefined || @password == ''

	#call remote login operation to get access token to be authorized user operations
	login: () ->
		if @checkForm()
			data = {
				email: @email,
				password: @password
			}
			options = @RestService.configureOptions(
				'POST', 
				jsRoutes.controllers.AuthenticationService.access_token().url.substring(1),
				undefined,
				data,
				false,
				undefined
			)
			@loginInternal(options)

	#checks form validity
	checkForm: () ->
		@alerts = []
		valid = true
		emailRegex = @Constants.EMAIL_REGEX

		#check for empty email input
		if @email == undefined || @email == ''
			@alerts.push({type:'danger', msg:"Please enter your email address."})
			valid = false
		#check for invalid email input
		else if !emailRegex.test(@email)
			@alerts.push({type:'danger', msg:"Please enter a valid email address."})
			valid = false
		#check for empty password input
		else if @password == undefined || @password == ''
			@alerts.push({type:'danger', msg:"Please enter your password."})
			valid = false

		valid

	#closes alert which is displayed due to an error
	closeAlert: (index) ->
		@alerts.splice(index, 1)

	#redirects user to the specified path, i.e. /login, /register, etc.
	redirect: (address) ->
		@$location.path(address);

controllers.controller('LoginController', LoginController)
