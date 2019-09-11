class ParameterDetailsController

	@$inject = ['$log', '$scope', '$uibModalInstance', 'parameter', 'ConfirmationDialogService']
	constructor: (@$log, @$scope, @$uibModalInstance, parameter, @ConfirmationDialogService) ->
		@$log.debug "Constructing ParameterDetailsController"
		@$scope.parameter = parameter

		@$scope.saveDisabled= () =>
			!(@$scope.parameter.name?.length > 0 && @$scope.parameter.kind?.length > 0)

		@$scope.updateParameter = () =>
			if !@$scope.saveDisabled()
				data = {}
				data.parameter = @$scope.parameter
				data.action = 'update'
				@$uibModalInstance.close(data)

		@$scope.deleteParameter = () =>
			@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this parameter?", "Yes", "No")
				.then () =>
					data = {}
					data.parameter = @$scope.parameter
					data.action = 'delete'
					@$uibModalInstance.close(data)

		@$scope.cancel = () =>
			@$uibModalInstance.dismiss()

@controllers.controller 'ParameterDetailsController', ParameterDetailsController
