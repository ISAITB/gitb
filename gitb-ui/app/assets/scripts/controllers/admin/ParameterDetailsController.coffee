class ParameterDetailsController

	@$inject = ['$log', '$scope', '$uibModalInstance', 'parameter', 'ConfirmationDialogService', 'options', 'ErrorService', 'Constants']
	constructor: (@$log, @$scope, @$uibModalInstance, parameter, @ConfirmationDialogService, options, @ErrorService, @Constants) ->
		@$log.debug "Constructing ParameterDetailsController"
		@$scope.parameter = _.cloneDeep parameter

		@$scope.nameLabel = if options.nameLabel? then options.nameLabel else 'Name'
		@$scope.hasKey = options.hasKey? && options.hasKey
		@$scope.modalTitle = if options.modalTitle? then options.modalTitle else 'Parameter details'
		@$scope.confirmMessage = if options.confirmMessage? then options.confirmMessage else 'Are you sure you want to delete this parameter?'
		@$scope.existingValues = options.existingValues

		@$scope.saveDisabled= () =>
			!(@$scope.parameter.name?.length > 0 && @$scope.parameter.kind?.length > 0 && (!@$scope.hasKey || @$scope.parameter.key?.length > 0))

		@$scope.updateParameter = () =>
			if !@$scope.saveDisabled() && @validName(@$scope.parameter.name) && @validKey(@$scope.parameter.key)
				data = {}
				data.parameter = @$scope.parameter
				data.action = 'update'
				@$uibModalInstance.close(data)

		@$scope.deleteParameter = () =>
			@ConfirmationDialogService.confirm("Confirm delete", @$scope.confirmMessage, "Yes", "No")
				.then () =>
					data = {}
					data.parameter = @$scope.parameter
					data.action = 'delete'
					@$uibModalInstance.close(data)

		@$scope.cancel = () =>
			@$uibModalInstance.dismiss()

	validName: (nameValue) =>
		finder = (value) =>
			_.find @$scope.existingValues, (v) => 
				v.id != @$scope.parameter.id && v.name == value
		if @$scope.existingValues? && finder(nameValue)
			nameToShow = @$scope.nameLabel.toLowerCase()
			@ErrorService.showSimpleErrorMessage('Invalid '+nameToShow, 'The provided '+nameToShow+' is already defined.')
			false
		else
			true

	validKey: (keyValue) =>
		result = false
		finder = (value) =>
			_.find @$scope.existingValues, (v) => 
				v.id != @$scope.parameter.id && v.key == value
		if @$scope.hasKey
			if @$scope.existingValues? && finder(keyValue)
				@ErrorService.showSimpleErrorMessage('Invalid key', 'The provided key is already defined.')
			else if !@Constants.VARIABLE_NAME_REGEX.test(keyValue)
				@ErrorService.showSimpleErrorMessage('Invalid key', 'The provided key is invalid. A key must begin with a character followed by zero or more characters, digits, or one of [\'.\', \'_\', \'-\'].')
			else
				result = true
		else
			result = true
		result

@controllers.controller 'ParameterDetailsController', ParameterDetailsController
