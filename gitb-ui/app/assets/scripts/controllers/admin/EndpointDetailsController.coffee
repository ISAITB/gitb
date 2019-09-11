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
				field: 'kindLabel'
				title: 'Type'
			}
			{
				field: 'useLabel'
				title: 'Required'
			}
			{
				field: 'adminOnlyLabel'
				title: 'Editable'
			}
			{
				field: 'notForTestsLabel'
				title: 'Included in tests'
			}
		]

		@ConformanceService.getEndpoints [@endpointId]
		.then (data) =>
			@endpoint = _.head data
			if @endpoint.parameters? && @endpoint.parameters.length > 0
				for e in @endpoint.parameters
					e.kindLabel = if e.kind == 'SIMPLE' then 'Simple' else if e.kind == 'BINARY' then 'Binary' else 'Hidden'
					e.useLabel = e.use == 'R'
					e.adminOnlyLabel = !e.adminOnly
					e.notForTestsLabel = !e.notForTests
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
			resolve:
				options: () => {}
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((parameter) => 
				@ConformanceService.createParameter parameter.name, parameter.description, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, @endpointId
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
				options: () => {}
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((data) => 
				if data.action == 'update'
					@ParameterService.updateParameter(data.parameter.id, data.parameter.name, data.parameter.desc, data.parameter.use, data.parameter.kind, data.parameter.adminOnly, data.parameter.notForTests, @endpointId)
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
