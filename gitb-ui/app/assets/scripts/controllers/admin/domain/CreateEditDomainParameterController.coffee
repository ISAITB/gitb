class CreateEditDomainParameterController

	@$inject = ['$log', '$scope', '$modalInstance', 'ConfirmationDialogService', 'ConformanceService', 'ErrorService', 'domainParameter', 'domainId']
	constructor:(@$log, @$scope, @$modalInstance, @ConfirmationDialogService, @ConformanceService, @ErrorService, domainParameter, domainId) ->
		@$log.debug "Constructing SystemController"

		@$scope.pending = false
		@$scope.savePending = false
		@$scope.deletePending = false
		@$scope.domainParameter = domainParameter
		@$scope.domainId = domainId
		@$scope.formData = {}
		@$scope.formData.initialKind = @$scope.domainParameter.kind
		@$scope.formData.showUpdateValue = @$scope.domainParameter.id? && @$scope.formData.initialKind == 'HIDDEN'
		@$scope.formData.updateValue = @$scope.formData.initialKind != 'HIDDEN'

		if domainParameter.id?
			@$scope.title = 'Update parameter'
		else 
			@$scope.title = 'Create parameter'
	
		@$scope.saveAllowed = () =>
			@$scope.domainParameter.name? && @$scope.domainParameter.kind? && 
				(@$scope.domainParameter.kind == 'SIMPLE' && @$scope.domainParameter.value?) ||
				(@$scope.domainParameter.kind == 'HIDDEN' && (!@$scope.formData.updateValue || (@$scope.formData.hiddenValue? && @$scope.formData.hiddenValueRepeat?)))

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
							
						@ConformanceService.updateDomainParameter(@$scope.domainParameter.id, @$scope.domainParameter.name, @$scope.domainParameter.description, @valueToSave, @$scope.domainParameter.kind, @$scope.domainId)
							.then((data) =>
									@$scope.pending = false
									@$scope.savePending = false
									@$modalInstance.close(data)
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
						@ConformanceService.createDomainParameter(@$scope.domainParameter.name, @$scope.domainParameter.description, @valueToSave, @$scope.domainParameter.kind, @$scope.domainId)
							.then((data) =>
								@$scope.pending = false
								@$scope.savePending = false
								@$modalInstance.close(data)
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
								@$modalInstance.close(data)
						, (error) =>
							@$scope.pending = false
							@$scope.deletePending = false
							@ErrorService.showErrorMessage(error)
						)
					console.log 'delete'

		@$scope.cancel = () =>
			@$modalInstance.dismiss()

controllers.controller('CreateEditDomainParameterController', CreateEditDomainParameterController)