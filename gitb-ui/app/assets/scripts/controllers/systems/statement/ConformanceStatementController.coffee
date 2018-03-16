class ConformanceStatementController

  @$inject = ['$log', '$scope', '$stateParams', '$state', '$modal', 'ConformanceService', 'SystemService', 'ErrorService', 'DataService']
  constructor: (@$log, @$scope, @$stateParams, @$state, @$modal, @ConformanceService, @SystemService, @ErrorService, @DataService)->
    @$log.debug "Constructing ConformanceStatementController"

    @conformanceStatements = []
    @conformanceStatementRepresentations = []

    @tableColumns = [
      {
        field: 'domainFull'
        title: 'Domain'
      }
      {
        field: 'specificationFull'
        title: 'Specification'
      }
      {
        field: 'actorFull'
        title: 'Actor'
      }
      {
        field: 'results'
        title: 'Results'
      }
    ]

    @getConformanceStatements()

  getConformanceStatements: () ->
    systemId = @$stateParams["id"]

    @SystemService.getConformanceStatements systemId
    .then (conformanceStatements) =>
      @conformanceStatements = conformanceStatements
      @conformanceStatementRepresentations = _.map conformanceStatements, (conformanceStatement) ->
        transformedObject =
          id: conformanceStatement.actorId
          actor: conformanceStatement.actor
          actorFull: conformanceStatement.actorFull
          specificationId: conformanceStatement.specificationId
          specification: conformanceStatement.specification
          specificationFull: conformanceStatement.specificationFull
          domainId: conformanceStatement.domainId
          domain: conformanceStatement.domain
          domainFull: conformanceStatement.domainFull
          results: "#{conformanceStatement.results.completed}/#{conformanceStatement.results.total} PASSED"

  onConformanceStatementSelect: (conformanceStatementRepresentation) =>
    @$state.go 'app.systems.detail.conformance.detail', {actor_id: conformanceStatementRepresentation.id, specId: conformanceStatementRepresentation.specificationId}

  showCreate: () =>
    !@DataService.isVendorUser

@controllers.controller 'ConformanceStatementController', ConformanceStatementController
