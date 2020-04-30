class ProvideInputModalController

	@$inject = ['$log', '$scope', '$uibModalInstance', 'Constants', 'TestService', 'session', 'interactionStepId', 'interactions', 'DataService']
	constructor:(@$log, @$scope, @$uibModalInstance, @Constants, @TestService, @session, @interactionStepId, interactions, @DataService) ->

		@$scope.interactions = interactions

		i = 0
		for interaction in @$scope.interactions
			if interaction.type == "request"
				if interaction.options?
					optionValues = interaction.options.split(',').map((item) =>
						item.trim()
					)
					optionLabelValues = interaction.optionLabels.split(',').map((item) =>
						item.trim()
					)
					interaction.optionData = []
					for optionValue, index in optionValues
						interaction.optionData.push({
							value: optionValue
							label: optionLabelValues[index]
						})
				if firstTextIndex == undefined
					firstTextIndex = i
			i += 1
		if firstTextIndex?
			@DataService.focus('input-'+firstTextIndex)

		@$scope.reset = () =>
			for interaction in @$scope.interactions
				delete interaction.data
				delete interaction.file

		@$scope.hideInput = () =>
			inputs = []
			for interaction in @$scope.interactions
				if interaction.type == "request"
					inputData = {
						id: interaction.id,
						name:  interaction.name
						type:  interaction.variableType,
						embeddingMethod: interaction.contentType
					}
					if interaction.optionData? && interaction.data?
						if interaction.multiple
							if Array.isArray(interaction.data)
								inputData.value = interaction.data.map((item) =>
									item.value
								)
								inputData.value = inputData.value.join()
						else
							inputData.value = interaction.data.value
					else
						inputData.value = interaction.data
					inputs.push(inputData)

			@TestService.provideInput(@session, @interactionStepId, inputs)
				.then(
					(data) =>
						@$log.debug data
						status = 
							success: true
						@$uibModalInstance.close(status)
				,
				(error) =>
					status = 
						success: false
						error: error
					@$uibModalInstance.close(status)
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
			if interaction.name?
				saveAs(blob, interaction.name)
			else
				@DataService.getFileInfo(blob).then((info) =>
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