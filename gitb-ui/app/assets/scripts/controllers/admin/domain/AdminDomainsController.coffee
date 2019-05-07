class AdminDomainsController

	@$inject = ['$log', '$scope', '$state', 'DataService', 'ConformanceService', 'ErrorService']
	constructor: (@$log, @$scope, @$state, @DataService, @ConformanceService, @ErrorService) ->
		@$log.debug "Constructing AdminDomainsController..."

		@tableColumns = [
			{
				field: 'sname',
				title: 'Short name'
			}
			{
				field: 'fname',
				title: 'Full name'
			}
			{
				field: 'description',
				title: 'Description'
			}
		]

		@domains = []
		@getDomains()

	getDomains: () ->
		if @DataService.isSystemAdmin
			@ConformanceService.getDomains()
			.then (data) =>
				@domains = data
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
		else if @DataService.isCommunityAdmin
			@ConformanceService.getCommunityDomain(@DataService.community.id)
			.then (data) =>
				@domains.push data if data
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	onDomainSelect: (domain)=>
		@$state.go 'app.admin.domains.detail.list', {id: domain.id}

@controllers.controller 'AdminDomainsController', AdminDomainsController
