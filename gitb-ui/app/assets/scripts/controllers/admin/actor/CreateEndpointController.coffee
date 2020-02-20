class CreateEndpointController

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService', 'DataService', 'PopupService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService, @DataService, @PopupService) ->
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id
		@actorId = @$stateParams.actor_id
		@endpoint = {}
		@DataService.focus('name')

	saveDisabled: () =>
		!(@endpoint.name?.length > 0)

	createEndpoint: () =>
		if !@saveDisabled()
			@ConformanceService.createEndpoint @endpoint.name, @endpoint.description, @actorId
				.then () =>
					@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
					@PopupService.success(@DataService.labelEndpoint()+' created.')
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: @actorId}
		
@controllers.controller 'CreateEndpointController', CreateEndpointController
