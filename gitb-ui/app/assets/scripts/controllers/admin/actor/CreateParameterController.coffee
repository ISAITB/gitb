class CreateParameterController

	@$inject = ['$log', '$scope', '$uibModalInstance', 'options', 'ErrorService', 'Constants', 'DataService']

	constructor: (@$log, @$scope, @$uibModalInstance, options, @ErrorService, @Constants, @DataService) ->
		@$scope.parameter = {}
		@$scope.parameter.use = 'O'
		@$scope.parameter.kind = 'SIMPLE'
		@$scope.parameter.notForTests = options.notForTests? && options.notForTests
		@$scope.parameter.adminOnly = options.adminOnly? && options.adminOnly
		@$scope.parameter.inExports = false
		@$scope.parameter.inSelfRegistration = false
		@$scope.parameter.hidden = false
		@$scope.parameter.hasPresetValues = false
		@$scope.parameter.presetValues = []

		@$scope.nameLabel = if options.nameLabel? then options.nameLabel else 'Name'
		@$scope.hasKey = options.hasKey? && options.hasKey
		@$scope.modalTitle = if options.modalTitle? then options.modalTitle else 'Create parameter'
		@$scope.existingValues = options.existingValues

		@$scope.dependsOnTargets = []
		@$scope.dependsOnTargetsMap = {}
		for v in @$scope.existingValues
			if v.kind == 'SIMPLE'
				@$scope.dependsOnTargets.push v
				@$scope.dependsOnTargetsMap[v.key] = v
		@$scope.reservedKeys = options.reservedKeys
		@$scope.hideInExport = options.hideInExport? && options.hideInExport
		@$scope.hideInRegistration = !@DataService.configuration['registration.enabled'] || (options.hideInRegistration? && options.hideInRegistration)

		@$uibModalInstance.rendered.then () => @DataService.focus('name')

		@$scope.saveDisabled = () =>
			!(@$scope.parameter.name?.length > 0 && @$scope.parameter.kind?.length > 0 && (!@$scope.hasKey || @$scope.parameter.key?.length > 0) && @presetValuesValid() && @dependencyValid())

		@$scope.createParameter = () =>
			if !@$scope.saveDisabled() && @validName(@$scope.parameter.name) && @validKey(@$scope.parameter.key)
				@$uibModalInstance.close(@$scope.parameter)

		@$scope.cancel = () =>
			@$uibModalInstance.dismiss()

		@$scope.removePresetValue = (index) =>
			@$scope.parameter.presetValues.splice(index, 1)

		@$scope.addPresetValue = () =>
			@$scope.parameter.presetValues.push({value: '', label:''})
			@DataService.focus('presetValue-'+(@$scope.parameter.presetValues.length - 1))

		@$scope.movePresetUp = (index) =>
			item = @$scope.parameter.presetValues.splice(index, 1)[0]
			@$scope.parameter.presetValues.splice(index-1, 0, item)

		@$scope.movePresetDown = (index) =>
			item = @$scope.parameter.presetValues.splice(index, 1)[0]
			@$scope.parameter.presetValues.splice(index+1, 0, item)

		@$scope.dependsOnChanged = () =>
			if @$scope.parameter.dependsOn == undefined || @$scope.parameter.dependsOn == ''
				@$scope.parameter.dependsOnValue = undefined

	presetValuesValid: () =>
		if @$scope.parameter.kind == 'SIMPLE' && @$scope.parameter.hasPresetValues
			for v in @$scope.parameter.presetValues
				if (v.value == undefined || v.value.trim().length == 0) || (v.label == undefined || v.label.trim().length == 0)
					return false
		return true

	dependencyValid: () =>
		valid = true
		if @$scope.parameter.dependsOn? && @$scope.parameter.dependsOn != ''
			if !@$scope.parameter.dependsOnValue? || @$scope.parameter.dependsOnValue.trim() == ''
				valid = false
		valid

	validName: (nameValue) =>
		finder = (value) =>
			_.find @$scope.existingValues, (v) => 
				v.name == value
		if @$scope.existingValues? && finder(nameValue)
			nameToShow = @$scope.nameLabel.toLowerCase()
			@ErrorService.showSimpleErrorMessage('Invalid '+nameToShow, 'The provided '+nameToShow+' is already defined.', 10)
			false
		else
			true

	validKey: (keyValue) =>
		result = false
		finder = (value) =>
			_.find @$scope.existingValues, (v) => 
				v.key == value
		finderReserved = (value) =>
			_.find @$scope.reservedKeys, (v) => 
				v == value
		if @$scope.hasKey
			if @$scope.existingValues? && finder(keyValue)
				@ErrorService.showSimpleErrorMessage('Invalid key', 'The provided key is already defined.')
			else if @$scope.reservedKeys? && finderReserved(keyValue)
				@ErrorService.showSimpleErrorMessage('Invalid key', 'The provided key is reserved.')
			else if !@Constants.VARIABLE_NAME_REGEX.test(keyValue)
				@ErrorService.showSimpleErrorMessage('Invalid key', 'The provided key is invalid. A key must begin with a character followed by zero or more characters, digits, or one of [\'.\', \'_\', \'-\'].')
			else
				result = true
		else
			result = true
		result

@controllers.controller 'CreateParameterController', CreateParameterController
