class EndpointDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'EndPointService', 'ParameterService', 'ConfirmationDialogService', '$state', '$stateParams', '$uibModal', 'ErrorService', 'DataService', 'PopupService']
	constructor: (@$log, @$scope, @ConformanceService, @EndPointService, @ParameterService, @ConfirmationDialogService, @$state, @$stateParams, @$uibModal, @ErrorService, @DataService, @PopupService) ->
		@$log.debug "Constructing EndpointDetailsController"
		@endpointId = @$stateParams.endpoint_id
		@actorId = @$stateParams.actor_id
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id
		@orderParametersDisabled = {value: true}

		@endpoint = null

		@ConformanceService.getEndpoints [@endpointId]
		.then (data) =>
			@endpoint = _.head data
			@parameterValues = []
			if @endpoint.parameters? && @endpoint.parameters.length > 0
				for e in @endpoint.parameters
					e.kindLabel = if e.kind == 'SIMPLE' then 'Simple' else if e.kind == 'BINARY' then 'Binary' else 'Secret'
					e.useLabel = e.use == 'R'
					e.adminOnlyLabel = !e.adminOnly
					e.notForTestsLabel = !e.notForTests
					itemRef = {id: e.id, name: e.name, key: e.name, kind: e.kind}
					itemRef.hasPresetValues = false
					if e.allowedValues?
						itemRef.presetValues = JSON.parse(e.allowedValues)
						itemRef.hasPresetValues = itemRef.presetValues?.length > 0
					@parameterValues.push itemRef


		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@DataService.focus('name')

	moveParameterUp: (index) =>
		item = @endpoint.parameters.splice(index, 1)[0]
		@orderParametersDisabled.value = false
		@endpoint.parameters.splice(index-1, 0, item)

	moveParameterDown: (index) =>
		item = @endpoint.parameters.splice(index, 1)[0]
		@orderParametersDisabled.value = false
		@endpoint.parameters.splice(index+1, 0, item)

	orderParameters: () =>
		ids = []
		for param in @endpoint.parameters
			ids.push param.id
		@ParameterService.orderParameters(@endpointId, ids)
		.then () =>
			@PopupService.success('Parameter ordering saved.')
			@orderParametersDisabled.value = true
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
			@orderParametersDisabled.value = true

	delete: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this "+@DataService.labelEndpointLower()+"?", "Yes", "No")
		.then () =>
			@EndPointService.deleteEndPoint(@endpointId)
			.then () =>
				@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
				@PopupService.success(@DataService.labelEndpoint()+' deleted.')
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveChanges: () =>
		@EndPointService.updateEndPoint(@endpointId, @endpoint.name, @endpoint.description, @actorId)
		.then () =>
			@PopupService.success(@DataService.labelEndpoint()+' updated.')
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	saveDisabled: () =>
		!(@endpoint?.name? && @endpoint.name.trim() != '')

	back: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}

	preparePresetValues: (parameter) =>
		parameter.allowedValues = undefined
		if parameter.kind == 'SIMPLE' && parameter.hasPresetValues
			checkedValues = []
			for value in parameter.presetValues
				existingValue = _.find(checkedValues, (v) => v.value == value.value)
				if existingValue == undefined
					checkedValues.push({value: value.value, label: value.label})
			parameter.allowedValues = JSON.stringify(checkedValues)

	addParameter: () =>
		modalOptions =
			templateUrl: 'assets/views/admin/domains/create-parameter-modal.html'
			controller: 'CreateParameterController as CreateParameterController'
			size: 'lg'
			resolve:
				options: () => {
					hideInExport: true,
					hideInRegistration: true
					existingValues: @parameterValues
				}
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((parameter) => 
				@preparePresetValues(parameter)
				@ConformanceService.createParameter parameter.name, parameter.description, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue, @endpointId
					.then () =>
						@$state.go(@$state.$current, null, { reload: true })
						@PopupService.success('Parameter created.')
					.catch (error) =>
						@ErrorService.showErrorMessage(error)
		, angular.noop)

	onParameterSelect: (parameter) =>
		modalOptions =
			templateUrl: 'assets/views/admin/domains/detail-parameter-modal.html'
			controller: 'ParameterDetailsController as ParameterDetailsController'
			resolve:
				parameter: () => parameter
				options: () => {
					hideInExport: true,
					hideInRegistration: true
					existingValues: @parameterValues
				}
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((data) => 
				if data.action == 'update'
					@preparePresetValues(data.parameter)
					@ParameterService.updateParameter(data.parameter.id, data.parameter.name, data.parameter.desc, data.parameter.use, data.parameter.kind, data.parameter.adminOnly, data.parameter.notForTests, data.parameter.hidden, data.parameter.allowedValues, data.parameter.dependsOn, data.parameter.dependsOnValue, @endpointId)
					.then () =>
						@$state.go(@$state.$current, null, { reload: true })
						@PopupService.success('Parameter updated.')
					.catch (error) =>
						@ErrorService.showErrorMessage(error)
				else
					@ParameterService.deleteParameter(data.parameter.id)
					.then () =>
						@$state.go(@$state.$current, null, { reload: true })
						@PopupService.success('Parameter deleted.')
					.catch (error) =>
						@ErrorService.showErrorMessage(error)
			, angular.noop)

@controllers.controller 'EndpointDetailsController', EndpointDetailsController
