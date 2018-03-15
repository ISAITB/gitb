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

@controllers.controller 'CreateConformanceStatementController', CreateConformanceStatementController
