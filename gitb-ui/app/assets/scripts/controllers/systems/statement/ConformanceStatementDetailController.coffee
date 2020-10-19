class ConformanceStatementDetailController

  @$inject = ['$log', '$scope', '$state', '$stateParams', '$uibModal', 'SystemService', 'ConformanceService', 'ErrorService', 'Constants', 'ConfirmationDialogService', 'DataService', 'ReportService', 'TestService', 'PopupService', '$sce', 'HtmlService', 'OrganizationService', '$window', '$q']
  constructor: (@$log, @$scope, @$state, @$stateParams, @$uibModal, @SystemService, @ConformanceService, @ErrorService, @Constants, @ConfirmationDialogService, @DataService, @ReportService, @TestService, @PopupService, @$sce, @HtmlService, @OrganizationService, @$window, @$q) ->
    @$log.debug "Constructing ConformanceStatementDetailController"

    @systemId = @$stateParams['id']
    @actorId = @$stateParams['actor_id']
    @specId  = @$stateParams['specId']
    @actor = null
    @domain = null
    @specification = null
    @endpoints = []
    @hasEndpoints = false
    @hasMultipleEndpoints = false
    @endpointRepresentations = []
    @configurations = []
    @testSuites = []
    @runTestClicked = false
    @endpointsExpanded = @$stateParams['editEndpoints']? && @$stateParams['editEndpoints']
    @testStatus = ''
    @backgroundMode = false
    @allTestsSuccessful = false
    @hasTests = false

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
        testCase.hasDocumentation = result.testCaseHasDocumentation
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
          currentTestSuite.hasDocumentation = result.testSuiteHasDocumentation
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
      @hasTests = failedCount > 0 || completedCount > 0
      for testSuiteId in testSuiteIds
        testSuiteResults.push(testSuiteData[testSuiteId])
      @testSuites = testSuiteResults
      @testStatus = @DataService.testStatusText(completedCount, failedCount, undefinedCount)
      @conformanceStatus = @DataService.conformanceStatusForTests(completedCount, failedCount, undefinedCount)
      @allTestsSuccessful = completedCount == totalCount
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
        if endpoint.parameters.length > 0
          endpointsTemp.push(endpoint)
      @endpoints = endpointsTemp
      @configurations = configurations
      @constructEndpointRepresentations()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)      

  checkPrerequisite: (parameterMap, repr) =>
    if !repr.checkedPrerequisites?
      if repr.dependsOn?
        otherPrerequisites = @checkPrerequisite(parameterMap, parameterMap[repr.dependsOn])
        valueCheck = parameterMap[repr.dependsOn].value == repr.dependsOnValue
        repr.prerequisiteOk = otherPrerequisites && valueCheck
      else
        repr.prerequisiteOk = true
      repr.checkedPrerequisites = true
    repr.prerequisiteOk

  constructEndpointRepresentations: () =>
    @endpointRepresentations = []
    for endpoint in @endpoints
      endpointRepr = {
        name: endpoint.name
        description: endpoint.description
        id: endpoint.id
      }
      parameterMap = {}
      endpointRepr.parameters = []
      for parameter in endpoint.parameters
        repr = _.cloneDeep parameter
        relevantConfig = _.find(@configurations, (config) => 
          Number(parameter.id) == Number(config.parameter) && Number(parameter.endpoint) == Number(config.endpoint)
        )
        if relevantConfig?
          repr.value = relevantConfig.value
          repr.configured = relevantConfig.configured
        else
          repr.configured = false
          repr.value = undefined

        if repr.configured
          if parameter.kind == 'BINARY'
            repr.fileName = parameter.name
            if relevantConfig.extension?
              repr.fileName += relevantConfig.extension
            repr.mimeType = relevantConfig.mimeType
          else if parameter.kind == 'SECRET'
            repr.value = '*****'
          else if parameter.kind == 'SIMPLE'
            if parameter.allowedValues?
              presetValues = JSON.parse(parameter.allowedValues)
              if presetValues?.length > 0
                foundPresetValue = _.find presetValues, (v) => `v.value == repr.value`
                if foundPresetValue?
                  repr.valueToShow = foundPresetValue.label

        parameterMap[parameter.name] = repr
        if !parameter.hidden || @DataService.isSystemAdmin || @DataService.isCommunityAdmin
          endpointRepr.parameters.push repr

      hasVisibleParameters = false
      for p in endpointRepr.parameters
        if @checkPrerequisite(parameterMap, p)
          hasVisibleParameters = true
      if hasVisibleParameters
        @endpointRepresentations.push endpointRepr
    @hasEndpoints = @endpointRepresentations.length > 0
    @hasMultipleEndpoints = @endpointRepresentations.length > 1

  onExpand: (testSuite) =>
    testSuite.expanded = !testSuite.expanded if !@runTestClicked
    @runTestClicked = false

  getOrganisation : () =>
    organisation = @DataService.vendor
    if @DataService.isCommunityAdmin || @DataService.isSystemAdmin
      organisation = JSON.parse(@$window.localStorage['organization'])
    organisation

  executeHeadless: (testCases) =>
    # Check configurations
    @organisationConfigurationChecked = @$q.defer()
    @systemConfigurationChecked = @$q.defer()
    @configurationChecked = @$q.defer()
    # Organisation parameter values.
    @OrganizationService.checkOrganisationParameterValues(@getOrganisation().id)
    .then (data) =>
      @organisationProperties = data
      @organisationConfigurationChecked.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    # System parameter values.
    @SystemService.checkSystemParameterValues(@systemId)
    .then (data) =>
      @systemProperties = data
      @systemConfigurationChecked.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    # Statement parameter values.
    @ConformanceService.checkConfigurations(@actorId, @systemId)
    .then (data) =>
      @endpointRepresentationsToCheck = data
      @configurationChecked.resolve()
    .catch (error) =>
      @ErrorService.showErrorMessage(error)
    # Check status once everything is loaded.
    @$q.all([@organisationConfigurationChecked.promise, @systemConfigurationChecked.promise, @configurationChecked.promise]).then(() =>
      @configurationValid = @DataService.isConfigurationValid(@endpointRepresentationsToCheck)
      @systemConfigurationValid = @DataService.isMemberConfigurationValid(@systemProperties)
      @organisationConfigurationValid = @DataService.isMemberConfigurationValid(@organisationProperties)
      if (!@configurationValid || !@systemConfigurationValid || !@organisationConfigurationValid)
        # Missing configuration.
        organisationPropertyVisibility = @DataService.checkPropertyVisibility(@organisationProperties)
        systemPropertyVisibility = @DataService.checkPropertyVisibility(@systemProperties)
        statementPropertyVisibility = @DataService.checkPropertyVisibility(@endpointRepresentationsToCheck[0].parameters)
        options =
          templateUrl: 'assets/views/systems/conformance/missing-configuration-modal.html'
          controller: 'MissingConfigurationModalController as controller'
          resolve:
            organisationProperties: () => @organisationProperties
            organisationConfigurationValid: () => @organisationConfigurationValid
            systemProperties: () => @systemProperties
            systemConfigurationValid: () => @systemConfigurationValid
            endpointRepresentations: () => @endpointRepresentationsToCheck
            configurationValid: () => @configurationValid
            organisationPropertyVisibility: () => organisationPropertyVisibility
            systemPropertyVisibility: () => systemPropertyVisibility
            statementPropertyVisibility: () => statementPropertyVisibility
          size: 'lg'
        instance = @$uibModal.open options
        instance.result
          .finally(angular.noop)
          .then((data) => 
            if data.action == 'statement'
              @endpointsExpanded = true
            else if data.action == 'organisation'
              if @DataService.isVendorUser || @DataService.isVendorAdmin
                @$state.go 'app.settings.organisation', {viewProperties: true}
              else
                organisation = @getOrganisation()
                if @DataService.vendor.id == organisation.id
                  @$state.go 'app.settings.organisation', {viewProperties: true}
                else
                  @OrganizationService.getOrganizationBySystemId(@systemId)
                  .then (org) =>
                    @$state.go 'app.admin.users.communities.detail.organizations.detail.list', {org_id: org.id, community_id: org.community, viewProperties: true}
                  .catch (error) =>
                    @ErrorService.showErrorMessage(error)
            else if data.action == 'system'
              if @DataService.isVendorUser
                @$state.go 'app.systems.detail.info', {id: Number(@systemId), viewProperties: true}
              else
                @$state.go 'app.systems.list', {id: Number(@systemId), viewProperties: true}
          , angular.noop)
      else 
        # Proceed with execution.
        testCaseIds = _.map(testCases, (test) =>
          test.id
        )
        @TestService.startHeadlessTestSessions(testCaseIds, @specId, @systemId, @actorId)
          .then(
            (data) =>
              if testCaseIds.length == 1
                @PopupService.success('Started test session. Check <b>Test Sessions</b> for progress.')
              else
                @PopupService.success('Started '+testCaseIds.length+' test sessions. Check <b>Test Sessions</b> for progress.')
            (error) =>
              @ErrorService.showErrorMessage(error).finally(angular.noop).then(angular.noop, angular.noop)
          )
    )

  onTestSelect: (test) =>
    if @backgroundMode
      @executeHeadless([test])
    else
      @DataService.setTestsToExecute [test]
      @$state.go 'app.tests.execution', {systemId: @systemId, actorId: @actorId, specId:@specId, testCaseId: test.id}

  onTestSuiteSelect: (testSuite) =>
    if (!testSuite?)
      testSuite = @testSuites[0]
    testsToExecute = []
    for testCase in testSuite.testCases
      testsToExecute.push testCase
    if @backgroundMode
      @executeHeadless(testsToExecute)
    else
      @DataService.setTestsToExecute testsToExecute
      @$state.go 'app.tests.execution', {systemId: @systemId, actorId: @actorId, specId:@specId, testSuiteId: testSuite.id}

  onParameterSelect: (parameter) =>

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
    @DataService.isSystemAdmin || @DataService.isCommunityAdmin || (@DataService.isVendorAdmin && @DataService.community.allowStatementManagement && (@DataService.community.allowPostTestStatementUpdates || !@hasTests))

  canEditParameter: (parameter) =>
    @DataService.isSystemAdmin || @DataService.isCommunityAdmin || (@DataService.isVendorAdmin && !parameter.adminOnly && (@DataService.community.allowPostTestStatementUpdates || !@hasTests))

  deleteConformanceStatement: () ->
    @ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this conformance statement?", "Yes", "No")
    .then () =>
      @SystemService.deleteConformanceStatement(@systemId, [@actorId])
      .then () =>
          @$state.go("app.systems.detail.conformance.list", {id: @systemId})
          @PopupService.success('Conformance statement deleted.')
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

  onExportConformanceCertificate:() =>
    @exportCertificatePending = true
    @ConformanceService.exportOwnConformanceCertificateReport(@actorId, @systemId)
    .then (data) =>
        blobData = new Blob([data], {type: 'application/pdf'});
        saveAs(blobData, "conformance_certificate.pdf");
        @exportCertificatePending = false
    .catch (error) =>
        @ErrorService.showErrorMessage(error)
        @exportCertificatePending = false

  canExportConformanceCertificate: () =>
    @allTestsSuccessful && (@DataService.isSystemAdmin || @DataService.isCommunityAdmin || @DataService.community.allowCertificateDownload)

  showDocumentation: (title, content) ->
    html = @$sce.trustAsHtml(content)
    @HtmlService.showHtml(title, html)

  showTestCaseDocumentation: (testCaseId) ->
    @ConformanceService.getTestCaseDocumentation(testCaseId)
    .then (data) =>
      @showDocumentation("Test case documentation", data)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  showTestSuiteDocumentation: (testSuiteId) ->
    @ConformanceService.getTestSuiteDocumentation(testSuiteId)
    .then (data) =>
      @showDocumentation("Test suite documentation", data)
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

@controllers.controller 'ConformanceStatementDetailController', ConformanceStatementDetailController
