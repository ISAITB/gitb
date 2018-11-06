class TestSuiteService
	@$inject = ['$log', 'RestService']

	constructor: (@$log, @RestService) ->
		@$log.debug "Constructing TestSuiteService..."

	undeployTestSuite: (testSuiteId) ->
		@RestService.delete
			path: jsRoutes.controllers.TestSuiteService.undeployTestSuite(testSuiteId).url
			authenticate: true

	downloadTestSuite: (testSuiteId) ->
		@RestService.get
			path: jsRoutes.controllers.TestSuiteService.downloadTestSuite(testSuiteId).url
			authenticate: true
			responseType: "arraybuffer"

	getTestSuites: (ids) ->
		if ids? and ids.length > 0
			@RestService.get
				path: jsRoutes.controllers.TestSuiteService.getTestSuites().url
				authenticate: true
				params:
					ids: ids.join ','
		else
			@RestService.get
				path: jsRoutes.controllers.TestSuiteService.getTestSuites().url
				authenticate: true

	getTestSuitesWithTestCases: () ->
		@RestService.get
			path: jsRoutes.controllers.TestSuiteService.getTestSuitesWithTestCases().url
			authenticate: true

services.service('TestSuiteService', TestSuiteService)