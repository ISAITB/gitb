class CreateEditDomainParameterController

	@$inject = ['$log', '$scope', '$uibModalInstance', 'ConfirmationDialogService', 'ConformanceService', 'ErrorService', 'domainParameter', 'domainId', 'DataService', 'PopupService']
	constructor:(@$log, @$scope, @$uibModalInstance, @ConfirmationDialogService, @ConformanceService, @ErrorService, domainParameter, domainId, @DataService, @PopupService) ->
		@$log.debug "Constructing SystemController"

		@$scope.pending = false
		@$scope.savePending = false
		@$scope.deletePending = false
		@$scope.domainParameter = _.cloneDeep domainParameter
		@$scope.domainId = domainId
		@$scope.formData = {}
		@$scope.formData.initialKind = @$scope.domainParameter.kind
		@$scope.formData.showUpdateValue = @$scope.domainParameter.id? && @$scope.formData.initialKind == 'HIDDEN'
		@$scope.formData.updateValue = @$scope.formData.initialKind != 'HIDDEN'
		@$uibModalInstance.rendered.then () => @DataService.focus('name')
		if @$scope.domainParameter.id? && @$scope.domainParameter.kind == 'BINARY'
			@$scope.formData.data = @$scope.domainParameter.value
			delete @$scope.domainParameter.value
			mimeType = @DataService.mimeTypeFromDataURL(@$scope.formData.data)
			blob = @DataService.b64toBlob(@DataService.base64FromDataURL(@$scope.formData.data), mimeType)
			extension = @DataService.extensionFromMimeType(mimeType)
			@$scope.initialFileName =  @$scope.domainParameter.name+extension

		if domainParameter.id?
			@$scope.title = 'Update parameter'
		else 
			@$scope.title = 'Create parameter'
	
		@$scope.saveAllowed = () =>
			@$scope.domainParameter.name? && @$scope.domainParameter.kind? && 
				((@$scope.domainParameter.kind == 'SIMPLE' && @$scope.domainParameter.value?) ||
				(@$scope.domainParameter.kind == 'BINARY' && @$scope.formData.data?) ||
				(@$scope.domainParameter.kind == 'HIDDEN' && (!@$scope.formData.updateValue || (@$scope.formData.hiddenValue? && @$scope.formData.hiddenValueRepeat?))))

		@$scope.save = () =>
			if @$scope.domainParameter.kind == 'HIDDEN' && @$scope.formData.hiddenValue != @$scope.formData.hiddenValueRepeat
				@$scope.errorMessage = 'The provided values must match.'
			else
				@$scope.errorMessage = ''
				if @$scope.saveAllowed()
					@$scope.pending = true
					@$scope.savePending = true
					if @$scope.domainParameter.id?
						# Update
						if @$scope.domainParameter.kind == 'HIDDEN' && @$scope.formData.updateValue
							@valueToSave = @$scope.formData.hiddenValue
						else if @$scope.domainParameter.kind == 'SIMPLE'
							@valueToSave = @$scope.domainParameter.value
						else if @$scope.domainParameter.kind == 'BINARY'
							@valueToSave = @$scope.formData.data
							
						@ConformanceService.updateDomainParameter(@$scope.domainParameter.id, @$scope.domainParameter.name, @$scope.domainParameter.description, @valueToSave, @$scope.domainParameter.kind, @$scope.domainId)
							.then((data) =>
									@$scope.pending = false
									@$scope.savePending = false
									@$uibModalInstance.close(data)
									@PopupService.success('Parameter updated.')

							, (error) =>
								@$scope.pending = false
								@$scope.savePending = false
								@ErrorService.showErrorMessage(error)
							)
					else
						# Create
						@valueToSave = @$scope.domainParameter.value
						if @$scope.domainParameter.kind == 'HIDDEN'
							@valueToSave = @$scope.formData.hiddenValue
						else if @$scope.domainParameter.kind == 'BINARY'
							@valueToSave = @$scope.formData.data
						@ConformanceService.createDomainParameter(@$scope.domainParameter.name, @$scope.domainParameter.description, @valueToSave, @$scope.domainParameter.kind, @$scope.domainId)
							.then((data) =>
								@$scope.pending = false
								@$scope.savePending = false
								@$uibModalInstance.close(data)
								@PopupService.success('Parameter created.')
							, (error) =>
								@$scope.pending = false
								@$scope.savePending = false
								@ErrorService.showErrorMessage(error)
							)

		@$scope.delete = () =>
			@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this parameter?", "Yes", "No")
				.then () =>
					@$scope.pending = true
					@$scope.deletePending = true
					@ConformanceService.deleteDomainParameter(@$scope.domainParameter.id, @$scope.domainId)
						.then((data) =>
								@$scope.pending = false
								@$scope.deletePending = false
								@$uibModalInstance.close(data)
								@PopupService.success('Parameter deleted.')
						, (error) =>
							@$scope.pending = false
							@$scope.deletePending = false
							@ErrorService.showErrorMessage(error)
						)

		@$scope.cancel = () =>
			@$uibModalInstance.dismiss()

		@$scope.onFileSelect = (files) =>
			tempFile = _.head files
			if tempFile?
				if tempFile.size >= (Number(@DataService.configuration['savedFile.maxSize']) * 1024)
					@ErrorService.showSimpleErrorMessage('File upload problem', 'The maximum allowed size for files is '+@DataService.configuration['savedFile.maxSize']+' KBs.')
				else
					reader = new FileReader()
					reader.readAsDataURL tempFile
					reader.onload = (event) =>
						@fileName = tempFile.name
						@$scope.formData.data = event.target.result
						@$scope.$apply()
					reader.onerror = (event) =>
						@ErrorService.showErrorMessage(error)

		@$scope.showFileName = () =>
			@fileName? || @$scope.formData.data?

		@$scope.fileName = () =>
			name = ''
			if (@fileName?)
				name = @fileName
			else if @$scope.formData.data?
				name = @$scope.initialFileName
			name

		@$scope.download = () =>
			mimeType = @DataService.mimeTypeFromDataURL(@$scope.formData.data)
			blob = @DataService.b64toBlob(@DataService.base64FromDataURL(@$scope.formData.data), mimeType)
			saveAs(blob, @$scope.fileName())

controllers.controller('CreateEditDomainParameterController', CreateEditDomainParameterController)