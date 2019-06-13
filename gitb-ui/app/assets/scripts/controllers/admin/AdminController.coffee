class AdminController
	@$inject = [
		'$log', '$scope', '$rootScope', '$state',
		'DataService', 'AccountService', 'Events',
		'Constants'
	]
	constructor: (@$log, @$scope, @$rootScope, @$state, @DataService, @AccountService, @Events, @Constants) ->
		@$log.debug 'Constructing AdminController...'
		@communityId = @DataService.community.id

		if !@DataService.isSystemAdmin and !@DataService.isCommunityAdmin
			@$log.debug 'User is not system admin, redirecting to the main page...'
			@$state.go 'app.home'

	toSessionDashboard: () =>
		@DataService.clearSearchState()

controllers.controller 'AdminController', AdminController
