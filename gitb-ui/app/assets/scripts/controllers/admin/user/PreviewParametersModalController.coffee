class PreviewParametersModalController

	@$inject = ['$uibModalInstance', 'parameters', 'modalTitle', 'hasRegistrationCase', 'Constants', 'DataService']
	constructor: (@$uibModalInstance, @parameters, @modalTitle, @hasRegistrationCase, @Constants, @DataService) ->
		@mode = 'user'
		@parameters = _.cloneDeep @parameters
		if !@DataService.configuration['registration.enabled']
			@hasRegistrationCase = false
		else
			@parametersForRegistration = []
			for param in @parameters
				if param.inSelfRegistration
					@parametersForRegistration.push param

	hasVisibleProperties: () =>
		properties = @parameters
		if @mode == 'registration'
			properties = @parametersForRegistration
		result = false
		if properties?.length > 0
			if @mode == 'admin'
				result = true
			else
				for prop in properties
					if !prop.hidden
						result = true
		result

	close: () =>
		@$uibModalInstance.dismiss()

@controllers.controller 'PreviewParametersModalController', PreviewParametersModalController
