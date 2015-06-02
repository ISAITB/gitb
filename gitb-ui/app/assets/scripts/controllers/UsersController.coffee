class UsersController

	constructor: (@$log, @$location, @AuthProvider, @AccountService, @DataService) ->
		@$log.debug "Constructing UsersController..."

		if !@AuthProvider.isAuthenticated()
            @$location.path('/login')

		@users = []

		@getVendorUsers()

	getVendorUsers: () ->
		@AccountService.getVendorUsers()
		.then(
			(data) =>
				@users = data
			,
			(error) =>
				@ErrorService.showErrorMessage(error)
		)

controllers.controller('UsersController', UsersController)