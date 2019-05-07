class TestSuiteService
	@$inject = ['$log', 'RestService', 'DataService']

	constructor: (@$log, @RestService, @DataService) ->
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

	getAllTestSuitesWithTestCases: () =>
		@RestService.get
			path: jsRoutes.controllers.TestSuiteService.getAllTestSuitesWithTestCases().url
			authenticate: true

	getTestSuitesWithTestCasesForCommunity: () =>
		@RestService.get
			path: jsRoutes.controllers.TestSuiteService.getTestSuitesWithTestCasesForCommunity(@DataService.community.id).url
			authenticate: true

	getTestSuitesWithTestCasesForSystem: (systemId) ->
		@RestService.get
			path: jsRoutes.controllers.TestSuiteService.getTestSuitesWithTestCasesForSystem(systemId).url
			authenticate: true

services.service('TestSuiteService', TestSuiteService)