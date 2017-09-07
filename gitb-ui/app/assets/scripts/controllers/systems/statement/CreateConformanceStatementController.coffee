class CreateConformanceStatementController

  @$inject = ['$log', '$location', '$q', '$window', '$scope', '$state', '$stateParams', 'ConformanceService', 'SystemService', 'ErrorService']
  constructor:(@$log, @$location, @$q, @$window, @$scope, @$state, @$stateParams, @ConformanceService, @SystemService, @ErrorService) ->
    @$log.debug "Constructing CreateConformanceStatementController"

    @alerts = []
    @domains = []
    @specs  = []
    @actors = []
    @options = []
    @test   = []
    @systemId    = @$stateParams["id"]
    @selectedDomain = null
    @selectedSpec  = null
    @selectedActors = []
    @selectedOptions = []

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
    ]

    @optionTableColumns = @tableColumns.concat [{field: 'actor', title: 'Actor'}]

    @actorTableColumns = [
      {
        field: 'actorId',
        title: 'ID'
      }
      {
        field: 'name',
        title: 'Name'
      }
      {
        field: 'description',
        title: 'Description'
      }
    ]

    @steps = [
      {
        id: 1
        title: 'Select Domain'
      }
      {
        id: 2
        title: 'Select Specification'
      }
      {
        id: 3
        title: 'Select Actors'
      }
      {
        id: 4
        title: 'Select Options'
      }
    ]

    @community = JSON.parse(@$window.localStorage['community'])
    @domainId = if @community.domainId? then [ @community.domainId ] else []

    @getDomains()

  onWizardNext: (step) =>
    if step.id == 1
      if @selectedDomain?
        @getSpecs @selectedDomain.id
        true
      else
        false
    else if step.id == 2
      if @selectedSpec?
        @getActors @selectedSpec.id
        true
      else
        false
    else if step.id == 3
      if @selectedActors.length > 0
        @getOptions @selectedActors
        true
      else
        false
    else if step.id == 4
      true
    else
      true

  onWizardBefore: (step) =>
    true

  onWizardFinish: () =>
    @saveConformanceStatement()
    .then () =>
      @$state.go "app.systems.detail.conformance.list", {id: @systemId}
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  onDomainSelect: (domain) =>
    @selectedDomain = domain

  onSpecificationSelect: (spec) =>
    @selectedSpec = spec

  onActorSelect: (actor) =>
    @selectedActors.push actor

  onActorDeselect: (actor) =>
    _.remove @selectedActors, (a)->
      actor.id == a.id

  onOptionSelect: (option) =>
    @selectedOptions.push option

  onOptionDeselect: (option) =>
    _.remove @selectedOptions, (a)->
      option.id == a.id

  nextStep: () =>
    @$scope.$broadcast 'wizard-directive:next'

  getDomains: () ->
    @domains = []

    @ConformanceService.getDomains(@domainId)
    .then(
      (data) =>
        @domains = data
        @$scope.$broadcast 'wizard-directive:start'
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  getSpecs: (domainId) ->
    @specs  = []
    @selectedDomain = domainId

    @ConformanceService.getSpecifications(domainId)
    .then(
      (data) =>
        @specs = data
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  getActors: (specId) ->
    @actors = []
    @selectedSpec = specId

    @ConformanceService.getActorsWithSpecificationId(specId)
    .then(
      (data) =>
        @actors = data
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  getOptions: (actors) ->
    @options = []

    ids = _.map @selectedActors, (actor) ->
      actor.id

    if ids.length > 0
      @ConformanceService.getOptions ids
      .then (result)=>
        @options = result
      .catch (error) =>
        @ErrorService.showErrorMessage(error)

  saveConformanceStatement: () ->
    promises = _.map @selectedActors, (actor)=>
      options = _.filter @selectedOptions, (option) ->
        option.actor == actor.id
      optionIds = _.map options, (option) ->
        option.id
      @SystemService.defineConformanceStatement @systemId, @selectedSpec, actor.id, optionIds

    @$q.all promises

class ConformanceStatementDetailController
  name: 'ConformanceStatementDetailController'

  @$inject = ['$log', '$scope', '$state', '$stateParams', '$modal', 'SystemService', 'ConformanceService', 'ErrorService', 'Constants']
  constructor: (@$log, @$scope, @$state, @$stateParams, @$modal, @SystemService, @ConformanceService, @ErrorService, @Constants) ->
    @$log.debug "Constructing #{@name}"

    @systemId = @$stateParams['id']
    @actorId = @$stateParams['actor_id']
    @specId  = @$stateParams['specId']
    @conformanceStatement = null
    @conformanceStatementRepr = null
    @domain = null
    @conformanceTests = []
    @interoperabilityTests = []
    @endpoints = []
    @endpointRepresentations = []
    @configurations = []

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

    @testsTableColumns = [
      {
        field: 'sname'
        title: 'Short Name'
      }
      {
        field: 'fname'
        title: 'Full Name'
      }
      {
        field: 'description'
        title: 'Description'
      }
      {
        field: 'result'
        title: 'Last Result'
      }
    ]

    @initalizeFields()

  initalizeFields: () =>
    @SystemService.getConformanceStatements @systemId, @specId, @actorId
    .then (data) =>
      @conformanceStatement = _.head data
      @constructConformanceStatementRepresentation @conformanceStatement
    .then () =>
      @ConformanceService.getDomains  [@conformanceStatement.actor.domain]
      .then (data) =>
        @domain = _.head data
    .then () =>
      optionIds = []
      if @conformanceStatement.options? and @conformanceStatement.options.length > 0
        optionIds = _.map @conformanceStatement.options, (o) => o.id

      #get conformance tests
      @ConformanceService.getTestCases @actorId, @specId, optionIds, @Constants.TEST_CASE_TYPE.CONFORMANCE
      .then (data) =>
        testCaseIds = _.map data, (testCase) -> testCase.id

        if testCaseIds.length > 0
          @SystemService.getLastExecutionResultsForTestCases @systemId, testCaseIds
          .then (results) =>
            for id, result of results
              test = _.find data, (test) =>
                `test.id == id`
              if test?
                test.result = result
            @conformanceTests = data

      #get interoperability tests
      @ConformanceService.getTestCases @actorId, @specId, optionIds, @Constants.TEST_CASE_TYPE.INTEROPERABILITY
        .then (data) =>
          testCaseIds = _.map data, (testCase) -> testCase.id

          if testCaseIds.length > 0
            @SystemService.getLastExecutionResultsForTestCases @systemId, testCaseIds
            .then (results) =>
              for id, result of results
                test = _.find data, (test) =>
                  `test.id == id`
                if test?
                  test.result = result
              @interoperabilityTests = data
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

  constructEndpointRepresentations: () =>
    @endpointRepresentations = _.map @endpoints, (endpoint) =>
        name: endpoint.name
        desc: endpoint.desc
        id: endpoint.id
        parameters: _.map endpoint.parameters, (parameter) =>
          repr = _.cloneDeep parameter
          repr.configured =  _.some @configurations, (configuration) =>
            parameter.id == configuration.parameter &&
              Number(parameter.endpoint) == Number(configuration.endpoint) &&
              configuration.value?
          repr

  onTestSelect: (test) =>
    @$state.go 'app.tests.execution', {test_id: test.sname, systemId: @systemId, actorId: @conformanceStatementRepr.id, specId:@specId}

  constructConformanceStatementRepresentation: (conformanceStatement) =>
    @conformanceStatementRepr =
      id: conformanceStatement.actor.id
      actorId: conformanceStatement.actor.actorId
      actorName: conformanceStatement.actor.name
      actorDescription: conformanceStatement.actor.description
      options: if conformanceStatement.options? conformanceStatement.options.length > 0 then (_.map conformanceStatement.options, (o) -> o.sname).join ', ' else '-'

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

  deleteConformanceStatement: () ->
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


@controllers.controller 'CreateConformanceStatementController', CreateConformanceStatementController
