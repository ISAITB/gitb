class ActorDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'ActorService', 'ConfirmationDialogService', '$state', '$stateParams', 'ErrorService', 'DataService', 'PopupService', 'Constants']
	constructor: (@$log, @$scope, @ConformanceService, @ActorService, @ConfirmationDialogService, @$state, @$stateParams, @ErrorService, @DataService, @PopupService, @Constants) ->
		@$log.debug "Constructing ActorDetailsController"

		@actor = {}
		@options = []
		@endpoints = []
		@endpointRepresentations = []
		@dataStatus = {status: @Constants.STATUS.PENDING}

		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id
		@actorId = @$stateParams.actor_id

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

		@endpointTableColumns = [
			{
				field: 'name'
				title: 'Name'
			}
			{
				field: 'desc'
				title: 'Description'
			}
			{
				field: 'parameters'
				title: 'Parameters'
			}
		]

		@ConformanceService.getActorsWithIds([@actorId])
		.then (data) =>
			@actor = _.head data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getEndpointsForActor(@actorId)
		.then (data) =>
			@endpoints = data
			@endpointRepresentations = _.map @endpoints, (endpoint) =>
				parameters = _.map endpoint.parameters, (parameter)->
					parameter.name

				repr =
					'id': endpoint.id
					'name': endpoint.name
					'desc': endpoint.description
					'parameters': parameters.join ', '
			@dataStatus.status = @Constants.STATUS.FINISHED
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
			@dataStatus.status = @Constants.STATUS.FINISHED

		@DataService.focus('id')

	delete: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this "+@DataService.labelActorLower()+"?", "Yes", "No")
		.then () =>
			@ActorService.deleteActor(@actorId)
			.then () =>
				@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}
				@PopupService.success(@DataService.labelActor()+' deleted.')
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveChanges: () =>
		@ActorService.updateActor(@actorId, @actor.actorId, @actor.name, @actor.description, @actor.default, @actor.hidden, @actor.displayOrder, @domainId, @specificationId)
		.then () =>
			@PopupService.success(@DataService.labelActor()+' updated.')
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	back: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}

	saveDisabled: () =>
		!(@actor?.actorId? && @actor?.name? && @actor.actorId.trim() != '' && @actor.name.trim() != '') || (@actor.displayOrder? && isNaN(@actor.displayOrder))

	onEndpointSelect: (endpoint) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.endpoints.detail', {id: @domainId, spec_id: @specificationId, actor_id: @actorId, endpoint_id: endpoint.id}


@controllers.controller 'ActorDetailsController', ActorDetailsController
