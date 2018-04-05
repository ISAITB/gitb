class ConformanceStatementDetailController

  @$inject = ['$log', '$scope', '$state', '$stateParams', '$modal', 'SystemService', 'ConformanceService', 'ErrorService', 'Constants', 'ConfirmationDialogService', 'DataService']
  constructor: (@$log, @$scope, @$state, @$stateParams, @$modal, @SystemService, @ConformanceService, @ErrorService, @Constants, @ConfirmationDialogService, @DataService) ->
    @$log.debug "Constructing ConformanceStatementDetailController"

    @systemId = @$stateParams['id']
    @actorId = @$stateParams['actor_id']
    @specId  = @$stateParams['specId']
    @actor = null
    @domain = null
    @specification = null
    @endpoints = []
    @endpointRepresentations = []
    @configurations = []
    @testSuites = []
    @runTestClicked = false

    @parameterTableColumns = [
      {
        field: 'name'
        title: 'Name'
      }
      {
        field: 'use'
        title: 'Usage'
      }
      {
        field: 'kind'
        title: 'Type'
      }
      {
        field: 'configured'
        title: 'Configured'
      }
    ]

    @optionTableColumns = [
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
    ]

    @initalizeFields()

  initalizeFields: () =>
    @ConformanceService.getActorTestSuites @specId, @actorId, @Constants.TEST_CASE_TYPE.CONFORMANCE
    .then (data) =>
      for testSuite in data
        testCaseIds = _.map testSuite.testCases, (testCase) -> testCase.id
        if testCaseIds.length > 0
           @SystemService.getLastExecutionResultsForTestSuite @systemId, testSuite.id, testCaseIds
           .then (result) =>
               ts = _.find data, (ts) =>
                 `ts.id == result.id`
               for id, result of result.testCases
                 tc = _.find ts.testCases, (tc) =>
                   `tc.id == id`
                 tc.result = result
               @determineTestSuiteResult(ts)
      @testSuites = data

    @ConformanceService.getActorsWithIds [@actorId]
    .then (data) =>
      @actor = _.head data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ConformanceService.getDomainForSpecification @specId
    .then (data) =>
      @domain = data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ConformanceService.getSpecificationsWithIds([@specId])
    .then (data) =>
      @specification = _.head data
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

    @ConformanceService.getEndpointsForActor @actorId
    .then (endpoints) =>
      @endpoints = endpoints
    .then () =>
      if @endpoints?.length > 0
        endpointIds = _.map @endpoints, (endpoint) -> endpoint.id
        @SystemService.getConfigurationsWithEndpointIds(endpointIds, @systemId)
        .then (configurations) =>
          @configurations = configurations
        .then () =>
          @constructEndpointRepresentations()
        .catch (error) =>
          @ErrorService.showErrorMessage(error)

  determineTestSuiteResult: (testSuite) =>
     overallResult = @Constants.TEST_CASE_RESULT.SUCCESS
     for testCase, i in testSuite.testCases
       if testCase.result == @Constants.TEST_CASE_RESULT.FAILURE
         overallResult = @Constants.TEST_CASE_RESULT.FAILURE
         break
       if testCase.result == @Constants.TEST_CASE_RESULT.UNDEFINED
         overallResult = @Constants.TEST_CASE_RESULT.UNDEFINED
     testSuite.result = overallResult

  constructEndpointRepresentations: () =>
    @endpointRepresentations = _.map @endpoints, (endpoint) =>
        name: endpoint.name
        description: endpoint.description
        id: endpoint.id
        parameters: _.map endpoint.parameters, (parameter) =>
          repr = _.cloneDeep parameter
          repr.configured =  _.some @configurations, (configuration) =>
            parameter.id == configuration.parameter &&
              Number(parameter.endpoint) == Number(configuration.endpoint) &&
              configuration.value?
          repr

  onExpand: (testSuite) =>
    testSuite.expanded = !testSuite.expanded if !@runTestClicked
    @runTestClicked = false

  onTestSelect: (test) =>
    @DataService.setTestsToExecute [test]
    @$state.go 'app.tests.execution', {systemId: @systemId, actorId: @actorId, specId:@specId, testCaseId: test.id}

  onTestSuiteSelect: (testSuite) =>
    if (!testSuite?)
      testSuite = @testSuites[0]
    testsToExecute = []
    for testCase in testSuite.testCases
      testsToExecute.push testCase
    @DataService.setTestsToExecute testsToExecute
    @$state.go 'app.tests.execution', {systemId: @systemId, actorId: @actorId, specId:@specId, testSuiteId: testSuite.id}

  onParameterSelect: (parameter) =>
    @$log.debug "Editing parameter: ", parameter

    oldConfiguration = _.find @configurations, (configuration) =>
      parameter.id == configuration.parameter &&
        configuration.value? &&
        Number(configuration.endpoint) == Number(parameter.endpoint)

    endpoint = _.find @endpoints, (endpoint) => Number(parameter.endpoint) == Number(endpoint.id)

    options =
      templateUrl: 'assets/views/systems/conformance/edit-config.html'
      controller: 'EditEndpointConfigurationController as editEndpointConfigurationCtrl'
      resolve:
        endpoint: () => endpoint
        parameter: () => parameter
        systemId: () => parseInt @systemId
        configuration: () => oldConfiguration
      size: 'sm'

    instance = @$modal.open options
    instance.result
    .then (result) =>
      switch result.operation
        when @Constants.OPERATION.ADD
          if result.configuration.value?
            @configurations.push result.configuration
        when @Constants.OPERATION.UPDATE
          if oldConfiguration? && result.configuration.value?
            oldConfiguration.value = result.configuration.value
        when @Constants.OPERATION.DELETE
          if oldConfiguration?
            _.remove @configurations, (configuration) =>
              configuration.parameter == oldConfiguration.parameter &&
                Number(configuration.endpoint) == Number(oldConfiguration.endpoint)

      @constructEndpointRepresentations()

  canDelete: () =>
    !@DataService.isVendorUser

  deleteConformanceStatement: () ->
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this conformance statement?", "Yes", "No")
    .then () =>
      @SystemService.deleteConformanceStatement(@systemId, [@actorId])
      .then () =>
          @$state.go("app.systems.detail.conformance.list", {id: @systemId})
      .catch (error) =>
          @ErrorService.showErrorMessage(error)

class EditEndpointConfigurationController
  name: 'EditEndpointConfigurationController'

  @$inject = ['$log', '$window', 'SystemService', 'Constants', '$modalInstance', 'systemId', 'endpoint', 'parameter', 'configuration']
  constructor: (@$log, @$window, @SystemService, @Constants, @$modalInstance, @systemId, @endpoint, @parameter, @oldConfiguration) ->
    @$log.debug "Constructing #{@name}"
    @file = null

    if !@oldConfiguration?
      @configuration =
        system: @systemId
        endpoint: @endpoint.id
        parameter: @parameter.id
    else
      @configuration = _.cloneDeep @oldConfiguration

    @isBinary = @parameter.kind == "BINARY"
    @isConfiugrationSet = @configuration.value?

  onFileSelect: (files) =>
    @file = _.head files

  closeDialog: () =>
    if !@oldConfiguration?
      @$modalInstance.close
        configuration: @configuration
        operation: @Constants.OPERATION.ADD
    else
      @$modalInstance.close
        configuration: @configuration
        operation: @Constants.OPERATION.UPDATE

  cancel: () =>
    @$modalInstance.dismiss()

  save: () =>
    if @parameter.kind == "SIMPLE"
      if @configuration.value?
        @SystemService.saveEndpointConfiguration @endpoint.id, @configuration
        .then () => @closeDialog()
        .catch (error) => @$modalInstance.dismiss error
    else if @parameter.kind == "BINARY"
      if @file?
        reader = new FileReader()
        reader.readAsDataURL @file
        reader.onload = (event) =>
          result = event.target.result

          @configuration.value = result
          @SystemService.saveEndpointConfiguration @endpoint.id, @configuration
          .then () => @closeDialog()
          .catch (error) => @$modalInstance.dismiss error
        reader.onerror = (event) =>
          @$log.error "An error occurred while reading the file: ", @file, event
          @$modalInstance.dismiss event

@controllers.controller 'ConformanceStatementDetailController', ConformanceStatementDetailController
