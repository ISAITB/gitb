class CreateActorController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService', 'DataService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService, @DataService) ->
		@$log.debug "Constructing CreateActorController..."

		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id

		@actor = {}

	saveDisabled: () =>
		!(@actor.actorId?.length > 0 && @actor.name?.length > 0) || (@actor.displayOrder? && isNaN(@actor.displayOrder))

	createActor: () =>
		if !@saveDisabled()
			@ConformanceService.createActor @actor.actorId, @actor.name, @actor.description, @actor.default, @actor.displayOrder, @domainId, @specificationId
				.then () =>
					@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}

@controllers.controller 'CreateActorController', CreateActorController
