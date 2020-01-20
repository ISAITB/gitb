class CreateDomainController
	name: 'CreateDomainController'

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', 'ErrorService', 'DataService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @ErrorService, @DataService) ->
		@$log.debug "Constructing #{@name}..."

		@domain = {}

	saveDisabled: () =>
		!(@domain.sname?.length > 0 and @domain.fname?.length > 0)

	createDomain: () =>
		if !@saveDisabled()
			@ConformanceService.createDomain @domain.sname, @domain.fname, @domain.description
				.then () =>
					@$state.go 'app.admin.domains.list'
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.list'

@controllers.controller 'CreateDomainController', CreateDomainController
