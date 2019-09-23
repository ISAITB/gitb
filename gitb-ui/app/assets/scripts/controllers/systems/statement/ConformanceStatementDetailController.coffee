class ConformanceStatementDetailController

  @$inject = ['$log', '$scope', '$state', '$stateParams', '$uibModal', 'SystemService', 'ConformanceService', 'ErrorService', 'Constants', 'ConfirmationDialogService', 'DataService', 'ReportService']
  constructor: (@$log, @$scope, @$state, @$stateParams, @$uibModal, @SystemService, @ConformanceService, @ErrorService, @Constants, @ConfirmationDialogService, @DataService, @ReportService) ->
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
    @endpointsCollapsed = @$stateParams['editEndpoints'] == undefined || !@$stateParams['editEndpoints']
    @testStatus = ''

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

    @initalizeFields()

  initalizeFields: () =>
    @ConformanceService.getConformanceStatus(@actorId, @systemId)
    .then (data) =>
      testSuiteResults = []
      testSuiteIds = []
      testSuiteData = {}
      completedCount = 0
      failedCount = 0
      undefinedCount = 0
      totalCount = 0
      for result in data
        testCase = {}
        testCase.id = result.testCaseId
        testCase.sname = result.testCaseName
        testCase.description = result.testCaseDescription
        testCase.result = result.result
        totalCount += 1
        if testCase.result == @Constants.TEST_CASE_RESULT.FAILURE
          failedCount += 1
        else if testCase.result == @Constants.TEST_CASE_RESULT.UNDEFINED
          undefinedCount += 1
        else 
          completedCount += 1
        if !testSuiteData[result.testSuiteId]?
          currentTestSuite = {}
          testSuiteIds.push(result.testSuiteId)
          currentTestSuite.id = result.testSuiteId
          currentTestSuite.sname = result.testSuiteName
          currentTestSuite.description = result.testSuiteDescription
          currentTestSuite.result = result.result
          currentTestSuite.testCases = []
          testSuiteData[result.testSuiteId] = currentTestSuite
        else
          if (testSuiteData[result.testSuiteId].result == @Constants.TEST_CASE_RESULT.SUCCESS)
            if testCase.result == @Constants.TEST_CASE_RESULT.FAILURE || testCase.result == @Constants.TEST_CASE_RESULT.UNDEFINED
              testSuiteData[result.testSuiteId].result = testCase.result
          else if (testSuiteData[result.testSuiteId].result == @Constants.TEST_CASE_RESULT.UNDEFINED)
            if testCase.result == @Constants.TEST_CASE_RESULT.FAILURE
              testSuiteData[result.testSuiteId].result = @Constants.TEST_CASE_RESULT.FAILURE
        testSuiteData[result.testSuiteId].testCases.push(testCase)
      for testSuiteId in testSuiteIds
        testSuiteResults.push(testSuiteData[testSuiteId])
      @testSuites = testSuiteResults
      @testStatus = @DataService.testStatusText(completedCount, failedCount, undefinedCount)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

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

    @ConformanceService.getSystemConfigurations(@actorId, @systemId)
    .then (systemConfigs) =>
      endpointsTemp = []
      configurations = []
      for endpointConfig in systemConfigs
        endpoint = {}
        endpoint.id = endpointConfig.id
        endpoint.name = endpointConfig.name
        endpoint.description = endpointConfig.description
        endpoint.parameters = []
        for parameterConfig in endpointConfig.parameters
          endpoint.parameters.push(parameterConfig)
          if parameterConfig.configured
            configurations.push({
              system: Number(@systemId),
              value: parameterConfig.value
              endpoint: endpointConfig.id
              parameter: parameterConfig.id
              mimeType: parameterConfig.mimeType
              extension: parameterConfig.extension
              configured: parameterConfig.configured
            })
        endpointsTemp.push(endpoint)
      @endpoints = endpointsTemp
      @configurations = configurations
      @constructEndpointRepresentations()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)      

  constructEndpointRepresentations: () =>
    @endpointRepresentations = _.map @endpoints, (endpoint) =>
        name: endpoint.name
        description: endpoint.description
        id: endpoint.id
        parameters: _.map endpoint.parameters, (parameter) =>
          repr = _.cloneDeep parameter

          relevantConfig = _.find(@configurations, (config) => 
            parameter.id == config.parameter && Number(parameter.endpoint) == Number(config.endpoint)
          );
          if relevantConfig?
            repr.value = relevantConfig.value
            repr.configured = relevantConfig.configured
          else
            repr.configured = false

          if repr.configured
            if parameter.kind == 'BINARY'
              repr.fileName = parameter.name
              if relevantConfig.extension?
                repr.fileName += relevantConfig.extension
              repr.mimeType = relevantConfig.mimeType
            else if parameter.kind == 'SECRET'
              repr.value = '*****'
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
        configuration.configured &&
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
      size: 'm'

    instance = @$uibModal.open options
    instance.result
      .finally(angular.noop)
      .then((result) =>
          switch result.operation
            when @Constants.OPERATION.ADD
              if result.configuration.configured
                @configurations.push result.configuration
            when @Constants.OPERATION.UPDATE
              if oldConfiguration? && result.configuration.configured
                oldConfiguration.value = result.configuration.value
                oldConfiguration.configured = result.configuration.configured
                oldConfiguration.mimeType = result.configuration.mimeType
                oldConfiguration.extension = result.configuration.extension
            when @Constants.OPERATION.DELETE
              if oldConfiguration?
                _.remove @configurations, (configuration) =>
                  configuration.parameter == oldConfiguration.parameter &&
                    Number(configuration.endpoint) == Number(oldConfiguration.endpoint)
          @constructEndpointRepresentations()
      , angular.noop)

  canDelete: () =>
    !@DataService.isVendorUser

  canEditParameter: (parameter) =>
    @DataService.isSystemAdmin || @DataService.isCommunityAdmin || (@DataService.isVendorAdmin && !parameter.adminOnly)

  deleteConformanceStatement: () ->
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this conformance statement?", "Yes", "No")
    .then () =>
      @SystemService.deleteConformanceStatement(@systemId, [@actorId])
      .then () =>
          @$state.go("app.systems.detail.conformance.list", {id: @systemId})
      .catch (error) =>
          @ErrorService.showErrorMessage(error)

  onExportConformanceStatement: () =>
    @exportPending = true
    choice = @ConfirmationDialogService.confirm("Report options", "Would you like to include the detailed test step results per test session?", "Yes, include step results", "No, summary only", true)
    choice.then(() => 
      @ReportService.exportConformanceStatementReport(@actorId, @systemId, true)
      .then (data) =>
          blobData = new Blob([data], {type: 'application/pdf'});
          saveAs(blobData, "conformance_report.pdf");
          @exportPending = false
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
        @exportPending = false
    , () => 
      @ReportService.exportConformanceStatementReport(@actorId, @systemId, false)
      .then (data) =>
          blobData = new Blob([data], {type: 'application/pdf'});
          saveAs(blobData, "conformance_report.pdf");
          @exportPending = false
      .catch (error) =>
        @ErrorService.showErrorMessage(error)
        @exportPending = false
    )

  toggleEndpointDisplay: () =>
    @endpointsCollapsed = !@endpointsCollapsed

@controllers.controller 'ConformanceStatementDetailController', ConformanceStatementDetailController
