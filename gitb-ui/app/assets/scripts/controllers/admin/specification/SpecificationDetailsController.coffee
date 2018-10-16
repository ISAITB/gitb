class SpecificationDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'TestSuiteService', 'ConfirmationDialogService', 'SpecificationService', '$state', '$stateParams', '$modal', 'PopupService', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @TestSuiteService, @ConfirmationDialogService, @SpecificationService, @$state, @$stateParams, @$modal, @PopupService, @ErrorService) ->
		@$log.debug "Constructing SpecificationDetailsController"

		@specification = {}
		@actors = []
		@testSuites = []
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id

		@tableColumns = [
			{
				field: 'sname',
				title: 'Short Name'
			}
			{
				field: 'fname',
				title: 'Full Name'
			}
			{
				field: 'description',
				title: 'Description'
			}
		]

		@testSuiteTableColumns = [
			{
				field: 'sname',
				title: 'Name'
			}
			{
				field: 'description',
				title: 'Description'
			}
			{
				field: 'version',
				title: 'Version'
			}
		]

		@actorTableColumns = [
			{
				field: 'actorId',
				title: 'ID'
			}
			{
				field: 'name',
				title: 'Name'
			}
			{
				field: 'description',
				title: 'Description'
			}
			{
				field: 'default',
				title: 'Default'
			}			
		]

		@ConformanceService.getSpecificationsWithIds([@specificationId])
		.then (data) =>
			@specification = _.head data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getActorsWithSpecificationId(@specificationId)
		.then (data)=>
			@actors = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getTestSuites(@specificationId)
		.then (data)=>
			@testSuites = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onFileSelect: (files) =>
		@$log.debug "Test suite zip files is selected to deploy: ", files
		if files.length > 0
			@ConformanceService.deployTestSuite @specificationId, files[0]
			.progress (event) =>
				@$log.debug "File upload: " + (100.0 * event.loaded / event.total) + " percent uploaded."
			.then (result) => 
				if (result && result.data)
					if (result.data.success)
						@showTestSuiteUploadResult(result.data)
						@$state.go(@$state.$current, null, { reload: true });
					else if (result.data.pendingFolderId && result.data.pendingFolderId.length > 0)
						# Pending 
						modalOptions =
							templateUrl: 'assets/views/components/test-suite-upload-pending-modal.html'
							controller: 'TestSuiteUploadPendingModalController as TestSuiteUploadPendingModalController'
							resolve:
								specificationId: () => @specificationId
								pendingFolderId: () => result.data.pendingFolderId
							size: 'lg'
						modalInstance = @$modal.open(modalOptions)
						modalInstance.result.then((pendingResolutionData) => 
							# Closed
							@showTestSuiteUploadResult(pendingResolutionData)
							@$state.go(@$state.$current, null, { reload: true });
						, () => 
							# Dismissed
							@ConformanceService.resolvePendingTestSuite(@$scope.specificationId, result.data.pendingFolderId, 'cancel')
						)
					else
						error = { 
							statusText: "Upload error",
							data: {error_description: "An error occurred while processing the test suite: "+result.data.errorInformation}
						}
						@ErrorService.showErrorMessage(error)
				else
					@ErrorService.showErrorMessage("An error occurred while processing the test suite: Response was empty")

	showTestSuiteUploadResult: (result) =>
		if (result.success)
			collect = (item, data) => 
				getAction = (itemAction) -> 
					if (itemAction == "update") 
						"(updated)"
					else if (itemAction == "add") 
						"(added)"
					else if (itemAction == "unchanged") 
						"(unchanged)"
					else 
						"(deleted)"

				getIndex = (itemType) -> 
					if (itemType == 'testSuite') 
						0
					else if (itemType == 'testCase') 
						1
					else if (itemType == 'actor') 
						2
					else if (itemType == 'endpoint') 
						3
					else 
						4
				append = (currentVal, newVal) ->
					if (!currentVal) 
						newVal
					else
						currentVal + ", " + newVal
				data[getIndex(item.type)].value = append(data[getIndex(item.type)].value, item.name+" "+getAction(item.action))
			data = [
				{label: "Test suite:"}, 
				{label: "Test cases:"},
				{label: "Actors:"},
				{label: "Endpoints:"},
				{label: "Parameters:"}
			]
			collect item, data for item in result.items
			@PopupService.show("Test suite upload overview", data)
		else
			data = []
			data.push {
				label: "Error:",
				value: result.errorInformation
			}
			@PopupService.show("Test suite upload failed", data)

	onTestSuiteDownload: (data) =>
		@TestSuiteService.downloadTestSuite data.id
		.then (data) =>
			blobData = new Blob([data], {type: 'application/zip'});
			saveAs(blobData, "test_suite.zip");
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onTestSuiteDelete: (data) =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this test suite?", "Yes", "No")
		.then () =>
			@TestSuiteService.undeployTestSuite data.id
			.then () =>
				@$state.go(@$state.$current, null, { reload: true });
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	addExistingActors: () =>
		@ConformanceService.getActorsWithDomainId(@specification.domain)
		.then (data) =>
			domainActors = data
			domainActors = _.filter domainActors, (domainActor) =>
				not _.some @actors, (existingActor) =>
					domainActor.id == existingActor.id

			if domainActors.length > 0
				options =
					templateUrl: 'assets/views/admin/domains/add-existing-actors.html'
					controller: 'AddExistingActorsController as addExistingActorsCtrl'
					resolve:
						specification: () =>	@specification
						existingActors: () => @actors
						domainActors: () => domainActors
					size: 'lg'

				instance = @$modal.open options
				instance.result
				.then (actors) =>
					if actors?
						_.forEach actors, (actor) =>
							@actors.push actor
				.catch () =>
					@$log.debug "An error occurred or the user dismissed the modal dialog"
			else
				@$log.debug "No additional actors that can be added exists in this domain."

		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onActorSelect: (actor) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: actor.id}

	deleteSpecification: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this specification?", "Yes", "No")
		.then () =>
			@SpecificationService.deleteSpecification(@specificationId)
			.then () =>
				@$state.go 'app.admin.domains.detail.list', {id: @domainId}
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveSpecificationChanges: () =>
		@SpecificationService.updateSpecification(@specificationId, @specification.sname, @specification.fname, @specification.urls, @specification. diagram, @specification.description, @specification.spec_type)
		.then () =>
			@$state.go 'app.admin.domains.detail.list', {id: @domainId}
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	saveDisabled: () =>
		!(@specification?.sname? && @specification?.fname?)

	back: () =>
		@$state.go 'app.admin.domains.detail.list', {id: @domainId}

@controllers.controller 'SpecificationDetailsController', SpecificationDetailsController
