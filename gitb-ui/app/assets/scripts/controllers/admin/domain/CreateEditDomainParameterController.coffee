class CreateEditDomainParameterController

	@$inject = ['$log', '$scope', '$modalInstance', 'ConfirmationDialogService', 'ConformanceService', 'ErrorService', 'domainParameter', 'domainId']
	constructor:(@$log, @$scope, @$modalInstance, @ConfirmationDialogService, @ConformanceService, @ErrorService, domainParameter, domainId) ->
		@$log.debug "Constructing SystemController"

		@$scope.pending = false
		@$scope.savePending = false
		@$scope.deletePending = false
		@$scope.domainParameter = domainParameter
		@$scope.domainId = domainId

		if domainParameter.id?
			@$scope.title = 'Update parameter'
		else 
			@$scope.title = 'Create parameter'
	
		@$scope.save = () =>
			if @$scope.domainParameter.name? && @$scope.domainParameter.value?
				@$scope.pending = true
				@$scope.savePending = true
				# Currently only simple text properties are supported
				@$scope.domainParameter.kind = "SIMPLE"
				if @$scope.domainParameter.id?
					# Update
					@ConformanceService.updateDomainParameter(@$scope.domainParameter.id, @$scope.domainParameter.name, @$scope.domainParameter.description, @$scope.domainParameter.value, @$scope.domainParameter.kind, @$scope.domainId)
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
					@ConformanceService.createDomainParameter(@$scope.domainParameter.name, @$scope.domainParameter.description, @$scope.domainParameter.value, @$scope.domainParameter.kind, @$scope.domainId)
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