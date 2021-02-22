class TestSuiteService
	@$inject = ['$log', 'RestService', 'DataService']

	constructor: (@$log, @RestService, @DataService) ->
		@$log.debug "Constructing TestSuiteService..."

	updateTestCaseMetadata: (testCaseId, name, description, documentation) ->
		@RestService.post({
			path: jsRoutes.controllers.TestSuiteService.updateTestCaseMetadata(testCaseId).url,
			data: {
				name: name
				description: description
				documentation: documentation
			}
			authenticate: true
		})

	updateTestSuiteMetadata: (testSuiteId, name, description, documentation, version) ->
		@RestService.post({
			path: jsRoutes.controllers.TestSuiteService.updateTestSuiteMetadata(testSuiteId).url,
			data: {
				name: name
				description: description
				documentation: documentation
				version: version
			}
			authenticate: true
		})

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

	getTestSuiteWithTestCases: (testSuiteId) =>
		@RestService.get
			path: jsRoutes.controllers.TestSuiteService.getTestSuiteWithTestCases(testSuiteId).url
			authenticate: true

	getTestCase: (testCaseId) =>
		@RestService.get
			path: jsRoutes.controllers.TestSuiteService.getTestCase(testCaseId).url
			authenticate: true

services.service('TestSuiteService', TestSuiteService)