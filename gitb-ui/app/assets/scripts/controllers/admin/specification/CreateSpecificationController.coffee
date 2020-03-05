class CreateSpecificationController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService', 'DataService', 'PopupService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService, @DataService, @PopupService) ->
		@$log.debug "Constructing CreateSpecificationController"
		@specification = {}
		@DataService.focus('shortName')

	saveDisabled: () =>
		!(@specification?.sname? and @specification?.fname?)

	createSpecification: ()=>
		if !@saveDisabled()
			domainId = @$stateParams.id
			@ConformanceService.createSpecification @specification.sname, @specification.fname, @specification.description, @specification.hidden, domainId
			.then () =>
				@$state.go 'app.admin.domains.detail.list'
				@PopupService.success(@DataService.labelSpecification()+' created.')
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.detail.list', {id: @$stateParams.id}


@controllers.controller 'CreateSpecificationController', CreateSpecificationController
