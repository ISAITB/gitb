class ParameterDetailsController

	@$inject = ['$log', '$scope', '$modalInstance', 'parameter']
	constructor: (@$log, @$scope, @$modalInstance, parameter) ->
		@$log.debug "Constructing ParameterDetailsController"
		@$scope.parameter = parameter

		@$scope.updateParameter = () =>
			if @$scope.parameter.name?.length > 0 && @$scope.parameter.use?.length > 0 && @$scope.parameter.kind?.length > 0
				data = {}
				data.parameter = @$scope.parameter
				data.action = 'update'
				@$modalInstance.close(data)

		@$scope.deleteParameter = () =>
			data = {}
			data.parameter = @$scope.parameter
			data.action = 'delete'
			@$modalInstance.close(data)

		@$scope.cancel = () =>
			@$modalInstance.dismiss()

@controllers.controller 'ParameterDetailsController', ParameterDetailsController
