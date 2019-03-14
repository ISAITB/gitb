class CreateParameterController

	@$inject = ['$log', '$scope', '$uibModalInstance']

	constructor: (@$log, @$scope, @$uibModalInstance) ->
		@$scope.parameter = {}
		@$scope.parameter.use = 'O'
		@$scope.parameter.kind = 'SIMPLE'

		@$scope.saveDisabled = () =>
			!(@$scope.parameter.name?.length > 0 && @$scope.parameter.use?.length > 0 && @$scope.parameter.kind?.length > 0)

		@$scope.createParameter = () =>
			if !@$scope.saveDisabled()
				@$uibModalInstance.close(@$scope.parameter)

		@$scope.cancel = () =>
			@$uibModalInstance.dismiss()

@controllers.controller 'CreateParameterController', CreateParameterController
