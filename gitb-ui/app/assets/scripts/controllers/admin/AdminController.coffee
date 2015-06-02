class AdminController
	@$inject = [
		'$log', '$scope', '$rootScope', '$state',
		'DataService', 'AccountService', 'Events',
		'Constants'
	]
	constructor: (@$log, @$scope, @$rootScope, @$state, @DataService, @AccountService, @Events, @Constants) ->
		@$log.debug 'Constructing AdminController...'

		if !@DataService.isSystemAdmin
			@$log.debug 'User is not system admin, redirecting to the main page...'
			@$state.go 'app.main'

controllers.controller 'AdminController', AdminController
