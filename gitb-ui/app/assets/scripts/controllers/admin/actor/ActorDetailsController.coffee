class ActorDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'ActorService', 'ConfirmationDialogService', '$state', '$stateParams', 'ErrorService', 'DataService']
	constructor: (@$log, @$scope, @ConformanceService, @ActorService, @ConfirmationDialogService, @$state, @$stateParams, @ErrorService, @DataService) ->
		@$log.debug "Constructing ActorDetailsController"

		@actor = {}
		@options = []
		@endpoints = []
		@endpointRepresentations = []

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
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	delete: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this "+@DataService.labelActorLower()+"?", "Yes", "No")
		.then () =>
			@ActorService.deleteActor(@actorId)
			.then () =>
				@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveChanges: () =>
		@ActorService.updateActor(@actorId, @actor.actorId, @actor.name, @actor.description, @actor.default, @actor.displayOrder, @domainId, @specificationId)
		.then () =>
			@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	back: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}

	saveDisabled: () =>
		!(@actor?.actorId? && @actor?.name?) || (@actor.displayOrder? && isNaN(@actor.displayOrder))

	onEndpointSelect: (endpoint) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.endpoints.detail', {id: @domainId, spec_id: @specificationId, actor_id: @actorId, endpoint_id: endpoint.id}


@controllers.controller 'ActorDetailsController', ActorDetailsController
