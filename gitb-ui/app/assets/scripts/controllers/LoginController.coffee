class LoginController

	@$inject = [
		'$log', '$scope', '$rootScope', '$location', '$http',
		'AuthService', 'AuthProvider', 'Events', 'Constants', 'ErrorService', 'RestService'
	]
	constructor: (@$log, @$scope, @$rootScope, @$location, @$http, @AuthService,
		@AuthProvider, @Events, @Constants, @ErrorService, @RestService) ->
		@$log.debug "Constructing LoginController..."
		if (@AuthProvider.isAuthenticated())
			@$location.path('/')

		@alerts = []	  # alerts to be displayed
		@spinner = false # spinner to be display while waiting response from the server

	#call remote login operation to get access token to be authorized user operations
	login: () ->
		if @checkForm()
			@spinner = true #start spinner before calling service operation

			data = {
				email: @$scope.email,
				password: @$scope.password
			}
			options = @RestService.configureOptions(
				'POST', 
				jsRoutes.controllers.AuthenticationService.access_token().url.substring(1),
				undefined,
				data,
				false,
				undefined
			)
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
						remember: if @$scope.rememberme? then @$scope.rememberme else false
					})
					@spinner = false #stop spinner
				(error) =>
					switch error.status
						when 401  # Unauthorized
							@alerts.push({type:'danger', msg:"Incorrect email or password."})
						else
							@ErrorService.showErrorMessage(error)
					@$scope.password = '' #clear password field
					@spinner = false		 #stop spinner
			)

	#checks form validity
	checkForm: () ->
		@alerts = []
		valid = true
		emailRegex = @Constants.EMAIL_REGEX

		#check for empty email input
		if @$scope.email == undefined || @$scope.email == ''
			@alerts.push({type:'danger', msg:"Please enter your email address."})
			valid = false
		#check for invalid email input
		else if !emailRegex.test(@$scope.email)
			@alerts.push({type:'danger', msg:"Please enter a valid email address."})
			valid = false
		#check for empty password input
		else if @$scope.password == undefined || @$scope.password == ''
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
