class CreateEndpointController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService) ->
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id
		@actorId = @$stateParams.actor_id
		@endpoint = {}

	createEndpoint: () =>
		if @endpoint.name?.length > 0
			@ConformanceService.createEndpoint @endpoint.name, @endpoint.description, @actorId
				.then () =>
					@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
		
@controllers.controller 'CreateEndpointController', CreateEndpointController
