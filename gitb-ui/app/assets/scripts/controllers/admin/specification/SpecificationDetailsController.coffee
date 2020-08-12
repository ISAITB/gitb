class SpecificationDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'ConfirmationDialogService', 'SpecificationService', '$state', '$stateParams', '$uibModal', 'PopupService', 'ErrorService', 'DataService']
	constructor: (@$log, @$scope, @ConformanceService, @ConfirmationDialogService, @SpecificationService, @$state, @$stateParams, @$uibModal, @PopupService, @ErrorService, @DataService) ->
		@$log.debug "Constructing SpecificationDetailsController"

		@specification = {}
		@actors = []
		@testSuites = []
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id
		@uploadPending = false

		@tableColumns = [
			{
				field: 'sname',
				title: 'Short name'
			}
			{
				field: 'fname',
				title: 'Full name'
			}
			{
				field: 'description',
				title: 'Description'
			}
		]

		@testSuiteTableColumns = [
			{
				field: 'identifier',
				title: 'ID'
			}
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
			{
				field: 'hidden',
				title: 'Hidden'
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

		@DataService.focus('shortName')

	hasErrorsOrWarnings: (report) =>
		if report?.counters?.errors? && report?.counters?.warnings?
			parseInt(report.counters.errors) > 0 || parseInt(report.counters.warnings) > 0
		else
			false
		
	showPendingOptions: (pendingFolderId) =>
		modalOptions =
			templateUrl: 'assets/views/components/test-suite-upload-pending-modal.html'
			controller: 'TestSuiteUploadPendingModalController as TestSuiteUploadPendingModalController'
			resolve:
				specificationId: () => @specificationId
				pendingFolderId: () => pendingFolderId
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((pendingResolutionData) => 
				# Closed
				@showTestSuiteUploadResult(pendingResolutionData)
				@$state.go(@$state.$current, null, { reload: true });
			, () => 
				# Dismissed
				@uploadPending = false
				@ConformanceService.resolvePendingTestSuite(@specificationId, pendingFolderId, 'cancel')
			)
	
	showTestSuiteValidationReport: (report, pendingFolderId, exists) =>
		modalOptions =
			templateUrl: 'assets/views/components/test-suite-validation-report-modal.html'
			controller: 'TestSuiteValidationReportModalController as controller'
			resolve:
				report: () => report
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((data) => 
				# Proceed
				if (exists)
					@showPendingOptions(pendingFolderId)
				else
					@ConformanceService.resolvePendingTestSuite(@specificationId, pendingFolderId, "proceed", "keep", "update")
					.then((pendingResolutionData) =>
						@showTestSuiteUploadResult(pendingResolutionData)
						@$state.go(@$state.$current, null, { reload: true });
					, () =>
						@uploadPending = false
					)
			, () =>
				# Cancel
				@uploadPending = false
				if (pendingFolderId?)
					@ConformanceService.resolvePendingTestSuite(@specificationId, pendingFolderId, "cancel")
			)

	onFileSelect: (files) =>
		@$log.debug "Test suite zip files is selected to deploy: ", files
		if files.length > 0
			@uploadPending = true
			@ConformanceService.deployTestSuite @specificationId, files[0]
			.then((result) => 
				if (result && result.data && result.data.validationReport)
					hasErrorsOrWarnings = @hasErrorsOrWarnings(result.data.validationReport)
					if (hasErrorsOrWarnings)
						@showTestSuiteValidationReport(result.data.validationReport, result.data.pendingFolderId, result.data.exists)
					else
						if (result.data.exists)
							@showPendingOptions(result.data.pendingFolderId)
						else if (result.data.success)
							@showTestSuiteUploadResult(result.data)
							@$state.go(@$state.$current, null, { reload: true });
						else
							error = { 
								statusText: "Upload error",
								data: {error_description: "An error occurred while processing the test suite: "+result.data.errorInformation}
							}
							@ErrorService.showErrorMessage(error)
							@uploadPending = false
				else
					@ErrorService.showErrorMessage("An error occurred while processing the test suite: Response was empty")
					@uploadPending = false
			, (error) =>
				@ErrorService.showErrorMessage(error)
				@uploadPending = false
			)

	showTestSuiteUploadResult: (result) =>
		@uploadPending = false
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
				{label: @DataService.labelActors()+":"},
				{label: @DataService.labelEndpoints()+":"},
				{label: "Parameters:"}
			]
			collect item, data for item in result.items
			@PopupService.show("Test suite upload overview", data, () => @PopupService.success('Test suite uploaded.'))
		else
			data = []
			data.push {
				label: "Error:",
				value: result.errorInformation
			}
			@PopupService.show("Test suite upload failed", data)

	onActorSelect: (actor) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: actor.id}

	onTestSuiteSelect: (testSuite) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.testsuites.detail.list', {id: @domainId, spec_id: @specificationId, testsuite_id: testSuite.id}

	deleteSpecification: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this "+@DataService.labelSpecificationLower()+"?", "Yes", "No")
		.then () =>
			@SpecificationService.deleteSpecification(@specificationId)
			.then () =>
				@$state.go 'app.admin.domains.detail.list', {id: @domainId}
				@PopupService.success(@DataService.labelSpecification()+' deleted.')
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveSpecificationChanges: () =>
		@SpecificationService.updateSpecification(@specificationId, @specification.sname, @specification.fname, @specification.description, @specification.hidden)
		.then () =>
			@PopupService.success(@DataService.labelSpecification()+' updated.')
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	saveDisabled: () =>
		!(@specification?.sname? && @specification?.fname? && @specification.sname.trim() != '' && @specification.fname.trim() != '')

	back: () =>
		@$state.go 'app.admin.domains.detail.list', {id: @domainId}

@controllers.controller 'SpecificationDetailsController', SpecificationDetailsController
