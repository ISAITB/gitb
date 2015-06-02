class CreateOptionController
	name: 'CreateOptionController'

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorServices) ->
		@$log.debug "Constructing #{@name}..."

		@option = {}

		@actorId = @$stateParams.actor_id

	createOption: () =>
		if @option.sname?.length > 0 and
		@option.fname?.length > 0
			@ConformanceService.createOption @option.sname, @option.fname, @option.description, @actorId
				.then () =>
					@$state.go 'app.admin.domains.detail.actors.detail.list'
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

@ControllerUtils.register @controllers, CreateOptionController