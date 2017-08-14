class AdminDomainsController

	@$inject = ['$log', '$scope', '$state', 'ConformanceService', 'ErrorService']
	constructor: (@$log, @$scope, @$state, @ConformanceService, @ErrorService) ->
		@$log.debug "Constructing AdminDomainsController..."

		@tableColumns = [
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

		@domains = []
		@getDomains()

	getDomains: () ->
		@ConformanceService.getDomains()
			.then (data) =>
				@domains = data
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	onDomainSelect: (domain)=>
		@$state.go 'app.admin.domains.detail.list', {id: domain.id}


@controllers.controller 'AdminDomainsController', AdminDomainsController
