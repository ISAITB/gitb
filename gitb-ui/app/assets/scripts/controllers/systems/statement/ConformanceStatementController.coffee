class ConformanceStatementController

  @$inject = ['$log', '$scope', '$stateParams', '$state', '$uibModal', 'ConformanceService', 'SystemService', 'ErrorService', 'DataService']
  constructor: (@$log, @$scope, @$stateParams, @$state, @$uibModal, @ConformanceService, @SystemService, @ErrorService, @DataService)->
    @$log.debug "Constructing ConformanceStatementController"

    @conformanceStatements = []
    @conformanceStatementRepresentations = []

    @tableColumns = [
      {
        field: 'domainFull'
        title: @DataService.labelDomain()
      }
      {
        field: 'specificationFull'
        title: @DataService.labelSpecification()
      }
      {
        field: 'actorFull'
        title: @DataService.labelActor()
      }
      {
        field: 'results'
        title: 'Results'
      }
    ]

    @getConformanceStatements()

  getConformanceStatements: () =>
    systemId = @$stateParams["id"]

    @SystemService.getConformanceStatements systemId
    .then (conformanceStatements) =>
      @conformanceStatements = conformanceStatements
      @conformanceStatementRepresentations = _.map conformanceStatements, (conformanceStatement) =>
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
          results: @DataService.testStatusText(Number(conformanceStatement.results.completed), Number(conformanceStatement.results.failed), Number(conformanceStatement.results.undefined))
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  onConformanceStatementSelect: (conformanceStatementRepresentation) =>
    @$state.go 'app.systems.detail.conformance.detail', {actor_id: conformanceStatementRepresentation.id, specId: conformanceStatementRepresentation.specificationId}

  showCreate: () =>
    @DataService.isSystemAdmin || @DataService.isCommunityAdmin || (@DataService.isVendorAdmin && @DataService.community.allowStatementManagement)

@controllers.controller 'ConformanceStatementController', ConformanceStatementController
