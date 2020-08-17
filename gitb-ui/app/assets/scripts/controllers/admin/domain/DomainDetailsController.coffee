class DomainDetailsController

	@$inject = ['$log', '$scope', '$state', '$stateParams', 'ConfirmationDialogService', 'ConformanceService', 'ErrorService', '$uibModal', 'DataService', 'PopupService']
	constructor: (@$log, @$scope, @$state, @$stateParams, @ConfirmationDialogService, @ConformanceService, @ErrorService, @$uibModal, @DataService, @PopupService) ->
		@$log.debug "Constructing DomainDetailsController..."

		@domain = {}
		@specifications = []
		@domainParameters = []
		@domainId = @$stateParams.id

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
			{
				field: 'hidden',
				title: 'Hidden'
			}
		]

		@ConformanceService.getDomains([@domainId])
		.then (data) =>
			@domain = _.head data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getSpecifications(@domainId)
		.then (data)=>
			@specifications = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getDomainParameters(@domainId)
		.then (data)=>
			@domainParameters = []
			for parameter in data
				if (parameter.kind == 'HIDDEN')
					parameter.valueToShow = "*****"
				else if (parameter.kind == 'BINARY')
					mimeType = @DataService.mimeTypeFromDataURL(parameter.value)
					blob = @DataService.b64toBlob(@DataService.base64FromDataURL(parameter.value), mimeType)
					extension = @DataService.extensionFromMimeType(mimeType)
					parameter.valueToShow =  parameter.name+extension
				else 
					parameter.valueToShow = parameter.value
				@domainParameters.push(parameter)
		.catch (error) =>
			@ErrorService.showErrorMessage(error)
		
		@DataService.focus('shortName')

	downloadParameter: (parameter) =>
		mimeType = @DataService.mimeTypeFromDataURL(parameter.value)
		blob = @DataService.b64toBlob(@DataService.base64FromDataURL(parameter.value), mimeType)
		extension = @DataService.extensionFromMimeType(mimeType)
		saveAs(blob, parameter.name+extension)

	deleteDomain: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this "+@DataService.labelDomainLower()+"?", "Yes", "No")
		.then () =>
			@ConformanceService.deleteDomain(@domainId)
			.then () =>
				@$state.go 'app.admin.domains.list'
				@PopupService.success(@DataService.labelDomain()+' deleted.')
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveDisabled: () =>
		!(@domain.sname? && @domain.sname.trim() != '' and @domain.fname? && @domain.fname.trim() != '')

	saveDomainChanges: () =>
		@ConformanceService.updateDomain(@domainId, @domain.sname, @domain.fname, @domain.description)
		.then () =>
			@PopupService.success(@DataService.labelDomain()+' updated.')
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	back: () =>
		@$state.go 'app.admin.domains.list'

	onSpecificationSelect: (specification) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: specification.id}

	onDomainParameterSelect: (domainParameter) =>
		modalOptions =
			templateUrl: 'assets/views/admin/domains/create-edit-domain-parameter-modal.html'
			controller: 'CreateEditDomainParameterController as parameterCtrl'
			resolve: 
				domainParameter: () => domainParameter
				domainId: () => @domain.id
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((result) => 
				@$state.go @$state.current, {}, {reload: true}
			, angular.noop)

	createDomainParameter: () =>
		modalOptions =
			templateUrl: 'assets/views/admin/domains/create-edit-domain-parameter-modal.html'
			controller: 'CreateEditDomainParameterController as parameterCtrl'
			resolve: 
				domainParameter: () => {}
				domainId: () => @domain.id
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((result) => 
				@$state.go @$state.current, {}, {reload: true}
			, angular.noop)

	uploadTestSuite: () =>
		modalOptions =
			templateUrl: 'assets/views/components/test-suite-upload-modal.html'
			controller: 'TestSuiteUploadModalController as controller'
			backdrop: 'static'
			keyboard: false
			resolve:
				availableSpecifications: () => @specifications
				testSuitesVisible: () => false
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result.finally(angular.noop).then(angular.noop, angular.noop)

	createSpecification: () =>
		@$state.go 'app.admin.domains.detail.specifications.create', {id: @domainId}

@controllers.controller 'DomainDetailsController', DomainDetailsController
