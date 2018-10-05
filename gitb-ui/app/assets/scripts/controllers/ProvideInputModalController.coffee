class ProvideInputModalController

	@$inject = ['$log', '$scope', '$modalInstance', 'Constants', 'TestService', 'session', 'interactionStepId', 'interactions', 'DataService']
	constructor:(@$log, @$scope, @$modalInstance, @Constants, @TestService, @session, @interactionStepId, interactions, @DataService) ->

		@$scope.interactions = interactions

		@$scope.hideInput = () =>
			inputs = []
			for interaction in @$scope.interactions
				if interaction.type == "request"
					inputs.push({
						id: interaction.id,
						name:  interaction.name
						value: interaction.data
						type:  interaction.variableType,
						embeddingMethod: interaction.contentType
					})

			@TestService.provideInput(@session, @interactionStepId, inputs)
				.then(
					(data) =>
						@$log.debug data
						status = 
							success: true
						@$modalInstance.close(status)
				,
				(error) =>
					status = 
						success: false
						error: error
					@$modalInstance.close(status)
			)

		@$scope.onFileSelect = (request, files) =>
			request.file = _.head files
			if request.file?
				reader = new FileReader()
				reader.readAsDataURL request.file
				reader.onload = (event) =>
					request.data = event.target.result

		@$scope.download = (interaction) =>
			blob = @DataService.b64toBlob(interaction.value)
			@DataService.getFileInfo(blob).then((info) =>
				if interaction.name?
					nameToUse = interaction.name
				else
					nameToUse = "file"
					if info.extension?
						nameToUse += '.'+info.extension
				saveAs(blob, nameToUse)
			)

		@$scope.isConfigurationDataURL = (configuration) =>
			@Constants.DATA_URL_REGEX.test(configuration)

		@$scope.interactionNeedsInput = () =>
			for interaction in @$scope.interactions
				if interaction.type == "request"
					return true
			return false

controllers.controller('ProvideInputModalController', ProvideInputModalController)