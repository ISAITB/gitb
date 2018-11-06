class DomainDetailsController

	@$inject = ['$log', '$scope', '$state', '$stateParams', 'ConfirmationDialogService', 'ConformanceService', 'ErrorService', '$modal']
	constructor: (@$log, @$scope, @$state, @$stateParams, @ConfirmationDialogService, @ConformanceService, @ErrorService, @$modal) ->
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
		]

		@parameterTableColumns = [
			{
				field: 'name',
				title: 'Name'
			}
			{
				field: 'description',
				title: 'Description'
			}
			{
				field: 'valueToShow',
				title: 'Value'
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
				else 
					parameter.valueToShow = parameter.value
				@domainParameters.push(parameter)
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	deleteDomain: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this domain?", "Yes", "No")
		.then () =>
			@ConformanceService.deleteDomain(@domainId)
			.then () =>
				@$state.go 'app.admin.domains.list'
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveDisabled: () =>
		!(@domain?.sname? && @domain?.fname?)

	saveDomainChanges: () =>
		@ConformanceService.updateDomain(@domainId, @domain.sname, @domain.fname, @domain.description)
		.then () =>
			@$state.go 'app.admin.domains.list'
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
		modalInstance = @$modal.open(modalOptions)
		modalInstance.result
			.then((result) => 
				@$state.go @$state.current, {}, {reload: true}
			)

	createDomainParameter: () =>
		modalOptions =
			templateUrl: 'assets/views/admin/domains/create-edit-domain-parameter-modal.html'
			controller: 'CreateEditDomainParameterController as parameterCtrl'
			resolve: 
				domainParameter: () => {}
				domainId: () => @domain.id
			size: 'lg'
		modalInstance = @$modal.open(modalOptions)
		modalInstance.result
			.then((result) => 
				@$state.go @$state.current, {}, {reload: true}
			)

@controllers.controller 'DomainDetailsController', DomainDetailsController
