class CreateConformanceStatementController

  @$inject = ['$log', '$location', '$q', '$window', '$scope', '$state', '$stateParams', 'ConformanceService', 'SystemService', 'ErrorService']
  constructor:(@$log, @$location, @$q, @$window, @$scope, @$state, @$stateParams, @ConformanceService, @SystemService, @ErrorService) ->
    @$log.debug "Constructing CreateConformanceStatementController"

    @alerts = []
    @domains = []
    @specs  = []
    @actors = []
    @test   = []
    @systemId    = @$stateParams["id"]
    @selectedDomain = null
    @selectedSpec  = null
    @selectedActors = []

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
        true
      else
        false
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

  nextStep: () =>
    @$scope.$broadcast 'wizard-directive:next'

  getDomains: () =>
    @domains = []

    @ConformanceService.getDomains(@domainId)
    .then(
      (data) =>
        @domains = data
        if @domains? && @domains.length == 1
          @onDomainSelect(@domains[0])
        @$scope.$broadcast 'wizard-directive:start'
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  getSpecs: (domainId) =>
    @specs  = []
    @selectedDomain = domainId

    @ConformanceService.getSpecifications(domainId)
    .then(
      (data) =>
        @specs = data
        if @specs? && @specs.length == 1
          @onSpecificationSelect(@specs[0])
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  getActors: (specId) =>
    @actors = []
    @selectedSpec = specId

    @ConformanceService.getActorsWithSpecificationId(specId)
    .then(
      (data) =>
        if data? 
          if data.length == 1
            @actors = data
            @onActorSelect(@actors[0])
          else if data.length > 1
            defaultActor = _.find(data, (actor) =>
              actor.default == true
            )
            if defaultActor?
              @actors.push(defaultActor)
              @onActorSelect(defaultActor)
            else
              @actors = data
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  saveConformanceStatement: () ->
    promises = _.map @selectedActors, (actor)=>
      @SystemService.defineConformanceStatement @systemId, @selectedSpec, actor.id, []

    @$q.all promises

  styleDefaultDomain: () =>
    if @domains?.length == 1
      'selected'
    else
      ''

  styleDefaultSpecification: () =>
    if @specs?.length == 1
      'selected'
    else
      ''

  styleDefaultActor: (rowActor) =>
    if @actors?.length == 1
      'selected'
    else
      ''

@controllers.controller 'CreateConformanceStatementController', CreateConformanceStatementController
