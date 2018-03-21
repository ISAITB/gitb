class CreateDomainController
	name: 'CreateDomainController'

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @ErrorService) ->
		@$log.debug "Constructing #{@name}..."

		@domain = {}

	createDomain: () =>
		if @domain.sname?.length > 0 and
		@domain.fname?.length > 0
			@ConformanceService.createDomain @domain.sname, @domain.fname, @domain.description
				.then () =>
					@$state.go 'app.admin.domains.list'
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.list'

@controllers.controller 'CreateDomainController', CreateDomainController
