class CreateActorController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService) ->
		@$log.debug "Constructing CreateActorController..."

		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id

		@actor = {}

	createActor: () =>
		if @actor.actorId?.length > 0 and
		@actor.name?.length > 0
			@ConformanceService.createActor @actor.actorId, @actor.name, @actor.description, @domainId, @specificationId
				.then () =>
					@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}

@controllers.controller 'CreateActorController', CreateActorController
