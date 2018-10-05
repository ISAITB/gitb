class ProvideInputModalController

	@$inject = ['$log', '$scope', '$modalInstance', 'Constants', 'TestService', 'session', 'interactionStepId', 'interactions']
	constructor:(@$log, @$scope, @$modalInstance, @Constants, @TestService, @session, @interactionStepId, interactions) ->

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
			blobData = @$scope.base64toBlob(interaction.value)
			nameToUse = "downloadedFile"
			if interaction.name?
				nameToUse = interaction.name
			saveAs(blobData, nameToUse);

		@$scope.isConfigurationDataURL = (configuration) =>
			@Constants.DATA_URL_REGEX.test(configuration)

		@$scope.interactionNeedsInput = () =>
			for interaction in @$scope.interactions
				if interaction.type == "request"
					return true
			return false

		@$scope.base64toBlob = (byteString) =>
			bytes = atob(byteString)
			bb = new Blob([bytes])
			# bb = new Blob([ab], {type: 'application/xml'})
			return bb

controllers.controller('ProvideInputModalController', ProvideInputModalController)