class CreateActorController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService', 'DataService', 'PopupService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService, @DataService, @PopupService) ->
		@$log.debug "Constructing CreateActorController..."

		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id

		@actor = {}
		@DataService.focus('id')

	saveDisabled: () =>
		!(@actor?.actorId? && @actor?.name? && @actor.actorId.trim() != '' && @actor.name.trim() != '') || (@actor.displayOrder? && isNaN(@actor.displayOrder))

	createActor: () =>
		if !@saveDisabled()
			@ConformanceService.createActor @actor.actorId, @actor.name, @actor.description, @actor.default, @actor.hidden, @actor.displayOrder, @domainId, @specificationId
				.then () =>
					@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}
					@PopupService.success(@DataService.labelActor()+' created.')
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}

@controllers.controller 'CreateActorController', CreateActorController
