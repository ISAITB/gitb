class CreateEndpointController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService', 'DataService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService, @DataService) ->
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id
		@actorId = @$stateParams.actor_id
		@endpoint = {}

	saveDisabled: () =>
		!(@endpoint.name?.length > 0)

	createEndpoint: () =>
		if !@saveDisabled()
			@ConformanceService.createEndpoint @endpoint.name, @endpoint.description, @actorId
				.then () =>
					@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
		
@controllers.controller 'CreateEndpointController', CreateEndpointController
