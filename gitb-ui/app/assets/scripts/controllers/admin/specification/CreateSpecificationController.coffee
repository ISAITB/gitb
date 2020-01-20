class CreateSpecificationController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService', 'DataService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService, @DataService) ->
		@$log.debug "Constructing CreateSpecificationController"

		@specification = {}

	saveDisabled: () =>
		!(@specification?.sname? and @specification?.fname?)

	createSpecification: ()=>
		if !@saveDisabled()
			domainId = @$stateParams.id
			@ConformanceService.createSpecification @specification.sname, @specification.fname, @specification.urls, @specification.diagram, @specification.description, @specification.spec_type, domainId
			.then () =>
				@$state.go 'app.admin.domains.detail.list'
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.detail.list', {id: @$stateParams.id}


@controllers.controller 'CreateSpecificationController', CreateSpecificationController
