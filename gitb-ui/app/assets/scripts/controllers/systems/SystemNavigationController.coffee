class SystemNavigationController

	constructor: (@$log, @$scope, @$stateParams, @$state, @SystemService, @DataService)->

		@count = 0

		@SystemService.getSystems()
		.then (data) =>
			count = data.length

@controllers.controller 'SystemNavigationController', SystemNavigationController