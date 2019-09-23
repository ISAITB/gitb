class CreateEditSystemController

	@$inject = ['$log', '$scope', '$uibModalInstance', 'ConfirmationDialogService', 'SystemService', 'ErrorService', 'system', 'organisationId', 'CommunityService', 'DataService', 'viewProperties']
	constructor:(@$log, @$scope, @$uibModalInstance, @ConfirmationDialogService, @SystemService, @ErrorService, system, organisationId, @CommunityService, @DataService, viewProperties) ->
		@$log.debug "Constructing SystemController"

		@$scope.pending = false
		@$scope.savePending = false
		@$scope.deletePending = false
		@$scope.system = system
		@$scope.organisationId = organisationId
		@$scope.otherSystems = []

		@$scope.propertyData = {
			properties: []
			edit: viewProperties
		}

		if system.id?
			@SystemService.getSystemParameterValues(system.id)
			.then (data) =>
				@$scope.propertyData.properties = data
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
		else
			@CommunityService.getSystemParameters(@DataService.community.id)
			.then (data) =>
				@$scope.propertyData.properties = data
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

		@SystemService.getSystemsByOrganization(organisationId).then(
			(data) =>
				if @$scope.system.id?
					for system in data
						if (system.id+'' != @$scope.system.id+'')
							@$scope.otherSystems.push(system)
				else
					@$scope.otherSystems = data
			, (error) =>
				@ErrorService.showErrorMessage(error)
		)

		if system.id?
			@$scope.title = 'Update system'
		else 
			@$scope.title = 'Create system'
	
		@$scope.saveEnabled = () =>
			@$scope.system.sname? && @$scope.system.fname? && @$scope.system.version?

		@$scope.doUpdate = () =>
			@$scope.pending = true
			@$scope.savePending = true
			@SystemService.updateSystem(@$scope.system.id, @$scope.system.sname, @$scope.system.fname, @$scope.system.description, @$scope.system.version, @$scope.organisationId, @$scope.system.otherSystem, @$scope.propertyData.edit, @$scope.propertyData.properties)
				.then((data) =>
						@$scope.pending = false
						@$scope.savePending = false
						@$uibModalInstance.close(data)
				, (error) =>
					@$scope.pending = false
					@$scope.savePending = false
					@ErrorService.showErrorMessage(error)
				)

		@$scope.save = () =>
			if @$scope.saveEnabled()
				if @$scope.system.id?
					# Update
					if @$scope.system.otherSystem? && @$scope.system.otherSystem.id?
						@ConfirmationDialogService.confirm("Confirm test setup copy", "Copying the test setup from another system will remove current conformance statements and test results. Are you sure you want to proceed?", "Yes", "No")
							.then(() =>
								@$scope.doUpdate()
							)
					else
						@$scope.doUpdate()
				else
					# Create
					@SystemService.registerSystemWithOrganization(@$scope.system.sname, @$scope.system.fname, @$scope.system.description, @$scope.system.version, @$scope.organisationId, @$scope.system.otherSystem, @$scope.propertyData.edit, @$scope.propertyData.properties)
						.then((data) =>
							@$scope.pending = false
							@$scope.savePending = false
							@$uibModalInstance.close(data)
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
								@$uibModalInstance.close(data)
						, (error) =>
							@$scope.pending = false
							@$scope.deletePending = false
							@ErrorService.showErrorMessage(error)
						)

		@$scope.cancel = () =>
			@$uibModalInstance.dismiss()

controllers.controller('CreateEditSystemController', CreateEditSystemController)