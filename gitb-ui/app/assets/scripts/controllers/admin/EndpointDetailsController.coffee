class EndpointDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'EndPointService', 'ParameterService', 'ConfirmationDialogService', '$state', '$stateParams', '$uibModal', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @EndPointService, @ParameterService, @ConfirmationDialogService, @$state, @$stateParams, @$uibModal, @ErrorService) ->
		@$log.debug "Constructing EndpointDetailsController"
		@endpointId = @$stateParams.endpoint_id
		@actorId = @$stateParams.actor_id
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id

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
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	delete: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this endpoint?", "Yes", "No")
		.then () =>
			@EndPointService.deleteEndPoint(@endpointId)
			.then () =>
				@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveChanges: () =>
		@EndPointService.updateEndPoint(@endpointId, @endpoint.name, @endpoint.description, @actorId)
		.then () =>
			@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	saveDisabled: () =>
		!(@endpoint?.name?)

	back: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}

	addParameter: () =>
		modalOptions =
			templateUrl: 'assets/views/admin/domains/create-parameter-modal.html'
			controller: 'CreateParameterController as CreateParameterController'
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((parameter) => 
				@ConformanceService.createParameter parameter.name, parameter.description, parameter.use, parameter.kind, @endpointId
					.then () =>
						@$state.go(@$state.$current, null, { reload: true });
					.catch (error) =>
						@ErrorService.showErrorMessage(error)
		, angular.noop)

	onParameterSelect: (parameter) =>
		modalOptions =
			templateUrl: 'assets/views/admin/domains/detail-parameter-modal.html'
			controller: 'ParameterDetailsController as ParameterDetailsController'
			resolve:
				parameter: () => parameter
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((data) => 
				if data.action == 'update'
					@ParameterService.updateParameter(data.parameter.id, data.parameter.name, data.parameter.desc, data.parameter.use, data.parameter.kind, @endpointId)
					.then () =>
						@$state.go(@$state.$current, null, { reload: true });
					.catch (error) =>
						@ErrorService.showErrorMessage(error)
				else
					@ParameterService.deleteParameter(data.parameter.id)
					.then () =>
						@$state.go(@$state.$current, null, { reload: true });
					.catch (error) =>
						@ErrorService.showErrorMessage(error)
			, angular.noop)

@controllers.controller 'EndpointDetailsController', EndpointDetailsController
