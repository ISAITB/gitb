class CreateDomainController
	name: 'CreateDomainController'

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', 'ErrorService', 'DataService', 'PopupService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @ErrorService, @DataService, @PopupService) ->
		@$log.debug "Constructing #{@name}..."

		@domain = {}
		@DataService.focus('shortName')

	saveDisabled: () =>
		!(@domain.sname? && @domain.sname.trim() != '' and @domain.fname? && @domain.fname.trim() != '')

	createDomain: () =>
		if !@saveDisabled()
			@ConformanceService.createDomain @domain.sname, @domain.fname, @domain.description
				.then () =>
					@$state.go 'app.admin.domains.list'
					@PopupService.success(@DataService.labelDomain()+' created.')
				.catch (error) =>
					@ErrorService.showErrorMessage(error)

	cancel: () =>
		@$state.go 'app.admin.domains.list'

@controllers.controller 'CreateDomainController', CreateDomainController
