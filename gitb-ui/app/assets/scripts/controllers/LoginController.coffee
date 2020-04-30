class LoginController

	@$inject = [
		'$log', '$rootScope', '$location', '$http', '$uibModal'
		'AuthService', 'AuthProvider', 'Events', 'Constants', 'ErrorService', 'RestService', 'DataService', '$cookies', '$window', 'CommunityService', 'ConfirmationDialogService', 'PopupService'
	]
	constructor: (@$log, @$rootScope, @$location, @$http, @$uibModal, @AuthService,
		@AuthProvider, @Events, @Constants, @ErrorService, @RestService, @DataService, @$cookies, @$window, @CommunityService, @ConfirmationDialogService, @PopupService) ->
		@$log.debug "Constructing LoginController..."
		if (@AuthProvider.isAuthenticated())
			@$location.path('/')

		@loginOption = @$cookies.get(@Constants.LOGIN_OPTION_COOKIE_KEY)
		if !@loginOption?
			@loginOption = @Constants.LOGIN_OPTION.NONE
		if (@loginOption == @Constants.LOGIN_OPTION.REGISTER && !@DataService.configuration['registration.enabled']) || (@loginOption == @Constants.LOGIN_OPTION.DEMO && !@DataService.configuration['demos.enabled']) || (@loginOption == @Constants.LOGIN_OPTION.MIGRATE && (!@DataService.configuration['sso.enabled'] || !@DataService.configuration['sso.inMigration']))
			# Invalid login option
			@loginOption = @Constants.LOGIN_OPTION.NONE
		@alerts = []	  # alerts to be displayed
		@spinner = false # spinner to be display while waiting response from the server
		@directLogin = false
		@createPending = false
		@selfRegData = {}

		if @loginOption == @Constants.LOGIN_OPTION.REGISTER
			if @DataService.configuration['sso.enabled']
				@createAccount(@loginOption)
		else if @loginOption == @Constants.LOGIN_OPTION.MIGRATE || @loginOption == @Constants.LOGIN_OPTION.LINK_ACCOUNT
			@createAccount(@loginOption)
		else if @loginOption == @Constants.LOGIN_OPTION.DEMO
			@directLogin = true
			@loginViaSelection(@DataService.configuration['demos.account'])
		else 
			if @DataService.actualUser?.accounts? && @DataService.actualUser.accounts.length == 1 && @loginOption != @Constants.LOGIN_OPTION.FORCE_CHOICE
				@directLogin = true
				@loginViaSelection(@DataService.actualUser.accounts[0].id)
			else if @loginOption == @Constants.LOGIN_OPTION.NONE && !@DataService.configuration['sso.enabled']
				@DataService.focus('email')

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
				selfRegOptions: () => @CommunityService.getSelfRegistrationOptions()
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result.finally(angular.noop).then(
			() => @createPending = false, 
			() => @createPending = false
		)

	loginViaSelection: (userId) =>
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

	loginViaCredentials: (userEmail, userPassword) ->
		data = {
			email: userEmail,
			password: userPassword
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

	loginInternal: (options) =>
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
						if @DataService.configuration['sso.enabled']
							# We need to re-login to EU Login.
							promise = @ConfirmationDialogService.invalidSessionNotification()
							promise
								.then(
									() =>
										@$rootScope.$emit(@Events.onLogout, {full: true})
									,
									angular.noop
								)
								.catch(angular.noop)
								.finally(angular.noop)
						else
							@alerts.push({type:'danger', msg:"Incorrect email or password."})
					else
						@ErrorService.showErrorMessage(error)
				@password = '' #clear password field
				@spinner = false		 #stop spinner
		)

	cancelLogin: () ->
		url = @$location.absUrl()
		@$window.location.href = url.substring(0, url.indexOf('app#!'))

	loginDisabled: () =>
		@spinner || !@textProvided(@email) || !@textProvided(@password)

	textProvided: (value) =>
		value? && value.trim().length > 0

	registerDisabled: () ->
		@spinner || !(@selfRegData.selfRegOption?.communityId? && 
			(@selfRegData.selfRegOption.selfRegType != @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN || @textProvided(@selfRegData.selfRegToken)) && 
			@textProvided(@selfRegData.orgShortName) && @textProvided(@selfRegData.orgFullName)
			@textProvided(@selfRegData.adminName) && @textProvided(@selfRegData.adminEmail) && @textProvided(@selfRegData.adminPassword) && @textProvided(@selfRegData.adminPasswordConfirm))

	register: () =>
		if @checkRegisterForm()
			@spinner = true
			if @selfRegData.selfRegOption.selfRegType == @Constants.SELF_REGISTRATION_TYPE.PUBLIC_LISTING_WITH_TOKEN
				token = @selfRegData.selfRegToken
			if @selfRegData.template?
				templateId = @selfRegData.template.id
			@CommunityService.selfRegister(@selfRegData.selfRegOption.communityId, token, @selfRegData.orgShortName, @selfRegData.orgFullName, templateId, @selfRegData.selfRegOption.organisationProperties, @selfRegData.adminName, @selfRegData.adminEmail, @selfRegData.adminPassword)
			.then (data) =>
				if data?.error_code?
					@alerts.push({type:'danger', msg:data.error_description})
					@spinner = false
				else
					# All ok.
					@loginViaCredentials(@selfRegData.adminEmail, @selfRegData.adminPassword)
					@PopupService.success("Registration successful.")
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
				@spinner = false

	#call remote login operation to get access token to be authorized user operations
	login: () ->
		if @checkLoginForm()
			@loginViaCredentials(@email, @password)

	#checks form validity
	checkLoginForm: () ->
		@alerts = []
		valid = true
		if !@Constants.EMAIL_REGEX.test(@email)
			@alerts.push({type:'danger', msg:"Please enter a valid email address."})
			valid = false
		valid

	checkRegisterForm: () ->
		@alerts = []
		valid = true
		if !@Constants.EMAIL_REGEX.test(@selfRegData.adminEmail)
			@alerts.push({type:'danger', msg:"Please enter a valid email address."})
			valid = false
		else if @selfRegData.adminPassword != @selfRegData.adminPasswordConfirm
			@alerts.push({type:'danger', msg:"Your password was not correctly confirmed."})
			valid = false
		valid

	#closes alert which is displayed due to an error
	closeAlert: (index) ->
		@alerts.splice(index, 1)

	#redirects user to the specified path, i.e. /login, /register, etc.
	redirect: (address) ->
		@$location.path(address);

controllers.controller('LoginController', LoginController)
