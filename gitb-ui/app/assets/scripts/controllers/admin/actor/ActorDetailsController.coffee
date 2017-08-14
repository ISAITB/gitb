class ActorDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'ActorService', 'ConfirmationDialogService', '$state', '$stateParams', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @ActorService, @ConfirmationDialogService, @$state, @$stateParams, @ErrorService) ->
		@$log.debug "Constructing ActorDetailsController"

		@actor = {}
		@options = []
		@endpoints = []
		@endpointRepresentations = []

		@domainId = @$stateParams.id
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

		@ConformanceService.getOptionsForActor(@actorId)
		.then (data) =>
			@options = data
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
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this actor?", "Yes", "No")
		.then () =>
			@ActorService.deleteActor(@actorId)
			.then () =>
				@$state.go 'app.admin.domains.detail.list', {id: @domainId}
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveChanges: () =>
		@ActorService.updateActor(@actorId, @actor.actorId, @actor.name, @actor.description)
		.then () =>
			@$state.go 'app.admin.domains.detail.list', {id: @domainId}
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onEndpointSelect: (endpoint) =>
		@$state.go 'app.admin.domains.detail.actors.detail.endpoints.detail', {id: @domainId, actor_id: @actorId, endpoint_id: endpoint.id}


@controllers.controller 'ActorDetailsController', ActorDetailsController
