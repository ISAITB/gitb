class CreateSpecificationController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService) ->
		@$log.debug "Constructing CreateSpecificationController"

		@specification = {}

	createSpecification: ()=>
		if @specification.sname? and	@specification.sname?

			domainId = @$stateParams.id

			@ConformanceService.createSpecification @specification.sname, @specification.fname, @specification.urls, @specification.diagram, @specification.description, @specification.spec_type, domainId
			.then () =>
				@$state.go 'app.admin.domains.detail.list'
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

@controllers.controller 'CreateSpecificationController', CreateSpecificationController
