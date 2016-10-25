class UserProfileController

	constructor: (@$log, @$scope, @$location, @DataService, @AccountService, @ErrorService, @Constants) ->

		@$log.debug "Constructing UserProfileController..."

		@ds = @DataService # shorten service name
		@alerts = []       # alerts to be displayed
		@spinner = false   # spinner to be display while waiting response from the server
		@edit = false      # for enabling edit mode
		@$scope.data = {}  # create a variable in scope for holding ng-if bindings
		@$scope.data.name  = @ds.user.name
		@$scope.data.email = @ds.user.email
		@$scope.data.role = @Constants.USER_ROLE_LABEL[@ds.user.role]

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
					@alerts.push({type:'success', msg:"Your name has been updated."})
					@ds.user.name = @$scope.data.name #update real value
					@spinner = false #stop spinner
					@cancelEdit()    #cancel edit mode
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