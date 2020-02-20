class UserProfileController

	@$inject = ['$log', '$scope', '$rootScope', '$location', 'DataService', 'AccountService', 'AuthService', 'ErrorService', 'Constants', 'Events', 'ConfirmationDialogService', '$cookies', 'PopupService']
	constructor: (@$log, @$scope, @$rootScope, @$location, @DataService, @AccountService, @AuthService, @ErrorService, @Constants, @Events, @ConfirmationDialogService, @$cookies, @PopupService) ->

		@$log.debug "Constructing UserProfileController..."

		@ds = @DataService # shorten service name
		@alerts = []       # alerts to be displayed
		@spinner = false   # spinner to be display while waiting response from the server
		@edit = false      # for enabling edit mode
		@$scope.data = {}  # create a variable in scope for holding ng-if bindings
		@$scope.data.name  = @ds.user.name
		@$scope.data.email = @ds.user.email
		@$scope.data.role = @Constants.USER_ROLE_LABEL[@ds.user.role]

	disconnect: () ->
		@ConfirmationDialogService.confirm("Confirmation", "Removing this role from your account will also end your current session. Are you sure you want to proceed?", "Yes", "No")
		.finally(angular.noop)
		.then () =>
			@AuthService.disconnectFunctionalAccount()
			.then(
				(data) => #success handler
					@$cookies.put(@Constants.LOGIN_OPTION_COOKIE_KEY, @Constants.LOGIN_OPTION.FORCE_CHOICE)
					@$rootScope.$emit(@Events.onLogout, {full: false, keepLoginOption: true})
					@PopupService.success("Role removed from your account.")
				,
				(error) => #error handler
					@ErrorService.showErrorMessage(error)
			)

	linkOtherRole: () ->
		@ConfirmationDialogService.confirm("Confirmation", "Before linking another role to your account your current session will be closed. Are you sure you want to proceed?", "Yes", "No")
		.finally(angular.noop)
		.then () =>
			@$cookies.put(@Constants.LOGIN_OPTION_COOKIE_KEY, @Constants.LOGIN_OPTION.LINK_ACCOUNT)
			@$rootScope.$emit(@Events.onLogout, {full: false, keepLoginOption: true})

	register: () ->
		@ConfirmationDialogService.confirm("Confirmation", "Before registering another "+@DataService.labelOrganisationLower()+" your current session will be closed. Are you sure you want to proceed?", "Yes", "No")
		.finally(angular.noop)
		.then () =>
			@$cookies.put(@Constants.LOGIN_OPTION_COOKIE_KEY, @Constants.LOGIN_OPTION.REGISTER)
			@$rootScope.$emit(@Events.onLogout, {full: false, keepLoginOption: true})

	#cancels edit mode and reverts back the changes
	cancelEdit: () ->
		@edit = false
		@$scope.data.name = @ds.user.name

	#enables edit mode
	editProfile: () ->
		@edit = true;

	updateProfile: () ->
		if @checkForm()
			@spinner = true #start spinner before calling service operation
			@AccountService.updateUserProfile(@$scope.data.name, null, null)
			.then(
				(data) => #success handler
					@ds.user.name = @$scope.data.name #update real value
					@spinner = false #stop spinner
					@cancelEdit()    #cancel edit mode
					@PopupService.success("Your name has been updated.")
				,
				(error) => #error handler
					@ErrorService.showErrorMessage(error)
					#stop spinner
					@spinner = false
					#cancel edit mode
					@cancelEdit()
			)

	#checks form validity
	checkForm: () ->
		@alerts = []
		valid = true

		if @$scope.data.name == undefined || @$scope.data.name == ''
			@alerts.push({type:'danger', msg:"Your name can not be empty."})
			@$scope.data.name = @ds.user.name
			valid = false

		valid

	#closes alert which is displayed due to an error
	closeAlert: (index) ->
		@alerts.splice(index, 1)

controllers.controller('UserProfileController', UserProfileController)