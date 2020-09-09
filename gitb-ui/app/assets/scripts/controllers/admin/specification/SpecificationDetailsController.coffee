class SpecificationDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'ConfirmationDialogService', 'SpecificationService', '$state', '$stateParams', '$uibModal', 'PopupService', 'ErrorService', 'DataService']
	constructor: (@$log, @$scope, @ConformanceService, @ConfirmationDialogService, @SpecificationService, @$state, @$stateParams, @$uibModal, @PopupService, @ErrorService, @DataService) ->
		@$log.debug "Constructing SpecificationDetailsController"

		@specification = {}
		@actors = []
		@testSuites = []
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id

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

	uploadTestSuite: () =>
		modalOptions =
			templateUrl: 'assets/views/components/test-suite-upload-modal.html'
			controller: 'TestSuiteUploadModalController as controller'
			backdrop: 'static'
			keyboard: false
			resolve:
				availableSpecifications: () => [@specification]
				testSuitesVisible: () => true
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result.finally(angular.noop).then(angular.noop, angular.noop)

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
