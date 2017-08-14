class ConformanceStatementController

  @$inject = ['$log', '$scope', '$stateParams', '$state', '$modal', 'ConformanceService', 'SystemService', 'ErrorService']
  constructor: (@$log, @$scope, @$stateParams, @$state, @$modal, @ConformanceService, @SystemService, @ErrorService)->
    @$log.debug "Constructing ConformanceStatementController"

    @conformanceStatements = []
    @conformanceStatementRepresentations = []

    @tableColumns = [
      {
        field: 'actorId'
        title: 'Actor ID'
      }
      {
        field: 'actorName'
        title: 'Actor Name'
      }
      {
        field: 'options'
        title: 'Options'
      }
      {
        field: 'specification'
        title: 'Specification'
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

        options = null
        if conformanceStatement.options? and conformanceStatement.options.length > 0
          optionNames = _.map conformanceStatement.options, (option) ->
            option.sname
          options = optionNames.join ', '

        transformedObject =
          id: conformanceStatement.actor.id
          actorId: conformanceStatement.actor.actorId
          actorName: conformanceStatement.actor.name
          specification: conformanceStatement.specification.fname
          specificationId: conformanceStatement.specification.id
          options: options
          results: "#{conformanceStatement.results.completed}/#{conformanceStatement.results.total} PASSED"

  onConformanceStatementSelect: (conformanceStatementRepresentation) =>
    @$state.go 'app.systems.detail.conformance.detail', {actor_id: conformanceStatementRepresentation.id, specId: conformanceStatementRepresentation.specificationId}


@controllers.controller 'ConformanceStatementController', ConformanceStatementController
