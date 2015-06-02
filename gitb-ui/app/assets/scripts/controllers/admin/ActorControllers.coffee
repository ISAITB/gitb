class CreateActorController
	name: 'CreateActorController'

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService) ->
		@$log.debug "Constructing #{@name}..."

		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id

		@actor = {}

	createActor: () =>
		if @actor.actorId?.length > 0 and
		@actor.name?.length > 0
			@ConformanceService.createActor @actor.actorId, @actor.name, @actor.description, @domainId, @specificationId
				.then () =>
					if @specificationId?
						@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: @specificationId}
					else
						@$state.go 'app.admin.domains.detail.list', {id: @domainId}
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

class ActorDetailsController
	name: 'ActorDetailsController'
	
	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService) ->
		@$log.debug "Constructing #{@name}"

		@actor = {}
		@options = []
		@endpoints = []
		@endpointRepresentations = []

		@domainId = @$stateParams.id
		@actorId = @$stateParams.actor_id

		@optionTableColumns = [
			{
				field: 'sname',
				title: 'Short Name'
			}
			{
				field: 'fname',
				title: 'Full Name'
			}
			{
				field: 'description',
				title: 'Description'
			}
		]

		@endpointTableColumns = [
			{
				field: 'name'
				title: 'Name'
			}
			{
				field: 'desc'
				title: 'Description'
			}
			{
				field: 'parameters'
				title: 'Parameters'
			}
		]

		@ConformanceService.getActorsWithIds([@actorId])
		.then (data) =>
			@actor = _.head data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getOptionsForActor(@actorId)
		.then (data) =>
			@options = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getEndpointsForActor(@actorId)
		.then (data) =>
			@endpoints = data
			@endpointRepresentations = _.map @endpoints, (endpoint) =>
				parameters = _.map endpoint.parameters, (parameter)->
					parameter.name

				repr =
					'id': endpoint.id
					'name': endpoint.name
					'desc': endpoint.desc
					'parameters': parameters.join ', '
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onEndpointSelect: (endpoint) =>
		@$state.go 'app.admin.domains.detail.actors.detail.endpoints.detail', {id: @domainId, actor_id: @actorId, endpoint_id: endpoint.id}

@ControllerUtils.register @controllers, ActorDetailsController
@ControllerUtils.register @controllers, CreateActorController