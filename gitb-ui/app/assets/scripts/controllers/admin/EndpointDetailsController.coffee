class EndpointDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'EndPointService', 'ConfirmationDialogService', '$state', '$stateParams']
	constructor: (@$log, @$scope, @ConformanceService, @EndPointService, @ConfirmationDialogService, @$state, @$stateParams) ->
		@$log.debug "Constructing EndpointDetailsController"
		@endpointId = @$stateParams.endpoint_id
		@actorId = @$stateParams.actor_id
		@domainId = @$stateParams.id

		@endpoint = null

		@parameterTableColumns = [
			{
				field: 'name'
				title: 'Name'
			}
			{
				field: 'desc'
				title: 'Description'
			}
			{
				field: 'use'
				title: 'Usage'
			}
			{
				field: 'kind'
				title: 'Type'
			}
		]

		@ConformanceService.getEndpoints [@endpointId]
		.then (data) =>
			@endpoint = _.head data

	delete: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this endpoint?", "Yes", "No")
		.then () =>
			@EndPointService.deleteEndPoint(@endpointId)
			.then () =>
				@$state.go 'app.admin.domains.detail.actors.detail.list', {id: @domainId, actor_id: @actorId}
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveChanges: () =>
		@EndPointService.updateEndPoint(@endpointId, @endpoint.name, @endpoint.description)
		.then () =>
			@$state.go 'app.admin.domains.detail.actors.detail.list', {id: @domainId, actor_id: @actorId}
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

@controllers.controller 'EndpointDetailsController', EndpointDetailsController
