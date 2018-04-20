class CreateEditSystemController

	@$inject = ['$log', '$scope', '$modalInstance', 'ConfirmationDialogService', 'SystemService', 'ErrorService', 'system', 'organisationId']
	constructor:(@$log, @$scope, @$modalInstance, @ConfirmationDialogService, @SystemService, @ErrorService, system, organisationId) ->
		@$log.debug "Constructing SystemController"

		@$scope.pending = false
		@$scope.savePending = false
		@$scope.deletePending = false
		@$scope.system = system
		@$scope.organisationId = organisationId
		if system.id?
			@$scope.title = 'Update system'
		else 
			@$scope.title = 'Create system'
	
		@$scope.save = () =>
			if @$scope.system.sname? && @$scope.system.fname? && @$scope.system.version?
				@$scope.pending = true
				@$scope.savePending = true
				if @$scope.system.id?
					# Update
					@SystemService.updateSystem(@$scope.system.id, @$scope.system.sname, @$scope.system.fname, @$scope.system.description, @$scope.system.version, @$scope.organisationId)
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
					@SystemService.registerSystemWithOrganization(@$scope.system.sname, @$scope.system.fname, @$scope.system.description, @$scope.system.version, @$scope.organisationId)
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
			@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this system?", "Yes", "No")
				.then () =>
					@$scope.pending = true
					@$scope.deletePending = true
					@SystemService.deleteSystem(@$scope.system.id, @$scope.organisationId)
						.then((data) =>
								@$scope.pending = false
								@$scope.deletePending = false
								@$modalInstance.close(data)
						, (error) =>
							@$scope.pending = false
							@$scope.deletePending = false
							@ErrorService.showErrorMessage(error)
						)

		@$scope.cancel = () =>
			@$modalInstance.dismiss()

controllers.controller('CreateEditSystemController', CreateEditSystemController)