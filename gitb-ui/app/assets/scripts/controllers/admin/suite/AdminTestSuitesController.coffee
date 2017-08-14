class AdminTestSuitesController

	@$inject = ['$log', '$scope', '$state', 'TestSuiteService', 'ConformanceService', 'ErrorService']
	constructor: (@$log, @$scope, @$state, @TestSuiteService, @ConformanceService, @ErrorService) ->
		@$log.debug "Constructing AdminTestSuitesController..."

		@tableColumns = [
			{
				field: 'sname',
				title: 'Short Name'
			}
			{
				field: 'fname',
				title: 'Full Name'
			}
			{
				field: 'description',
				title: 'Description'
			}
			{
				field: 'specification',
				title: 'Specification'
			}
		]

		@testSuites = []
		@specifications = []
		@testSuiteRepresentations = []
		@getTestSuites()

	getTestSuites: () ->
		@TestSuiteService.getTestSuites()
		.then (data) => # request test suites
			@testSuites = data
		.then () => # extract specification ids
			_ @testSuites
			.map (testSuite)->
				testSuite.specification
			.unique()
			.value()
		.then (specificationIds) => # request specifications
			@ConformanceService.getSpecificationsWithIds(specificationIds)
		.then (specifications) => # store specifications
			@specifications = specifications
		.then () => #construct test suite representations
			@testSuiteRepresentations = _.map @testSuites, (testSuite) =>
				specification = _.find @specifications, (specification) => specification.id == testSuite.specification
				repr =
					id: testSuite.id
					sname: testSuite.sname
					fname: testSuite.fname
					description: testSuite.description
					specification: specification.sname
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onTestSuiteSelect: (testSuite)=>
		@$state.go 'app.admin.suites.detail', {id: testSuite.id}


@controllers.controller 'AdminTestSuitesController', AdminTestSuitesController
