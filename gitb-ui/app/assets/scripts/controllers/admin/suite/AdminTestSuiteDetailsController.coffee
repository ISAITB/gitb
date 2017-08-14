class AdminTestSuiteDetailsController

	@$inject = ['$log', '$scope', '$state', 'TestSuiteService', 'ConformanceService']
	constructor: (@$log, @$scope, @$state, @TestSuiteService, @ConformanceService) ->
		@$log.debug "Constructing AdminTestSuiteDetailsController..."

@controllers.controller 'AdminTestSuiteDetailsController', AdminTestSuiteDetailsController
