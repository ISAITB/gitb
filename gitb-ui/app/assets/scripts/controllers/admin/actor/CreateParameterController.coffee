class CreateParameterController

	@$inject = ['$log', '$scope', '$modalInstance']

	constructor: (@$log, @$scope, @$modalInstance) ->
		@$scope.parameter = {}
		@$scope.parameter.use = 'O'
		@$scope.parameter.kind = 'SIMPLE'

		@$scope.saveDisabled = () =>
			!(@$scope.parameter.name?.length > 0 && @$scope.parameter.use?.length > 0 && @$scope.parameter.kind?.length > 0)

		@$scope.createParameter = () =>
			if !@$scope.saveDisabled()
				@$modalInstance.close(@$scope.parameter)

		@$scope.cancel = () =>
			@$modalInstance.dismiss()

@controllers.controller 'CreateParameterController', CreateParameterController
