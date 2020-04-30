class CreateConformanceStatementController

  @$inject = ['$log', '$location', '$q', '$window', '$scope', '$state', '$stateParams', 'ConformanceService', 'SystemService', 'ErrorService', 'DataService', 'PopupService']
  constructor:(@$log, @$location, @$q, @$window, @$scope, @$state, @$stateParams, @ConformanceService, @SystemService, @ErrorService, @DataService, @PopupService) ->
    @$log.debug "Constructing CreateConformanceStatementController"

    @alerts = []
    @domains = []
    @specs  = []
    @actors = []
    @test   = []
    @systemId    = @$stateParams["id"]
    @selectedDomain = null
    @selectedSpec  = null
    @selectedActor = null
    @showDomains = false
    @showSpecs = false
    @showActors = false
    @showButtonPanel = false
    @counter = 0

    @tableColumns = [
      {
        field: 'sname',
        title: 'Short name'
      }
      {
        field: 'fname',
        title: 'Full name'
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
        title: 'Select ' + @DataService.labelDomainLower()
      }
      {
        id: 2
        title: 'Select ' + @DataService.labelSpecificationLower()
      }
      {
        id: 3
        title: 'Select ' + @DataService.labelActorLower()
      }
    ]

    @community = JSON.parse(@$window.localStorage['community'])
    @domainId = if @community.domainId? then [ @community.domainId ] else []

    @getDomains()

  finish: () =>
    @SystemService.defineConformanceStatement(@systemId, @selectedSpec.id, @selectedActor.id, [])
    .then (data) =>
      if data? && data.error_description?
        error = {
          statusText: "Error"
          data: {
            error_description: data.error_description
          }
        }      
        @ErrorService.showErrorMessage(error)
      else
        @$state.go "app.systems.detail.conformance.list", {id: @systemId}
        @PopupService.success("Conformance statement created.")
    .catch (error) =>
      @ErrorService.showErrorMessage(error)

  onDomainSelect: (domain) =>
    @selectedDomain = domain

  onSpecificationSelect: (spec) =>
    @selectedSpec = spec

  onActorSelect: (actor) =>
    @selectedActor = actor

  setDomainsView: () =>
    @counter += 1
    @showDomains = true
    @confirmAction = @confirmDomain
    @confirmButtonText = 'Next'
    @headerText = 'Select ' + @DataService.labelDomainLower()
    @showButtonPanel = true

  setSpecsView: () =>
    @counter += 1
    @showSpecs = true
    @confirmAction = @confirmSpec
    @confirmButtonText = 'Next'
    @headerText = 'Select ' + @DataService.labelSpecificationLower()
    @showButtonPanel = true

  setActorsView: () =>
    @counter += 1
    @showActors = true
    @confirmAction = @confirmActor
    @confirmButtonText = 'Next'
    @headerText = 'Select ' + @DataService.labelActorLower()
    @showButtonPanel = true

  setConfirmationView: () =>
    @counter += 1
    @showConfirmation = true
    @confirmAction = @confirmAll
    @confirmButtonText = 'Confirm'
    @headerText = 'Summary'
    @showButtonPanel = true

  confirmDisabled: () =>
    if @showDomains
      !@selectedDomain?
    else if @showSpecs
      !@selectedSpec?
    else if @showActors
      !@selectedActor?
    else
      false

  getDomains: () =>
    @domains = []

    @ConformanceService.getDomains(@domainId)
    .then(
      (data) =>
        @domains = data
        if @domains? && @domains.length == 1
          @onDomainSelect(@domains[0])
          @confirmDomain()
        else
           @setDomainsView()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  getSpecs: (domainId) =>
    @specs  = []

    @ConformanceService.getSpecifications(domainId)
    .then(
      (data) =>
        if data?
          _.remove(data, (spec) =>
            spec.hidden == true
          )          
          @specs = data
          if data.length == 1
            @onSpecificationSelect(@specs[0])
            @confirmSpec()
          else
            @setSpecsView()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  getActors: (specId) =>
    @actors = []

    @ConformanceService.getActorsWithSpecificationId(specId)
    .then(
      (data) =>
        if data? 
          _.remove(data, (actor) =>
            actor.hidden == true
          )
          if data.length == 1
            @actors = data
            @onActorSelect(@actors[0])
            @confirmActor()
          else if data.length > 1
            defaultActor = _.find(data, (actor) =>
              actor.default == true
            )
            if defaultActor?
              @onActorSelect(defaultActor)
              @confirmActor()
            else
              @actors = data
              @setActorsView()
          else
            @setActorsView()
      ,
      (error) =>
        @ErrorService.showErrorMessage(error)
    )

  confirmDomain: () =>
    @showDomains = false
    @getSpecs(@selectedDomain.id)

  confirmSpec: () =>
    @showSpecs = false
    @getActors(@selectedSpec.id)

  confirmActor: () =>
    @showActors = false
    @setConfirmationView()

  confirmAll: () =>
    @finish()

  cancel: () =>
    @$state.go "app.systems.detail.conformance.list", {id: @systemId}

@controllers.controller 'CreateConformanceStatementController', CreateConformanceStatementController
