class AdminDomainsController

	@$inject = ['$log', '$scope', '$state', 'DataService', 'ConformanceService', 'ErrorService', 'Constants']
	constructor: (@$log, @$scope, @$state, @DataService, @ConformanceService, @ErrorService, @Constants) ->
		@$log.debug "Constructing AdminDomainsController..."
		@dataStatus = {status: @Constants.STATUS.PENDING}
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
				@dataStatus.status = @Constants.STATUS.FINISHED
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
				@dataStatus.status = @Constants.STATUS.FINISHED
		else if @DataService.isCommunityAdmin
			@ConformanceService.getCommunityDomain(@DataService.community.id)
			.then (data) =>
				@domains.push data if data
				@dataStatus.status = @Constants.STATUS.FINISHED
			.catch (error) =>
				@ErrorService.showErrorMessage(error)
				@dataStatus.status = @Constants.STATUS.FINISHED

	onDomainSelect: (domain)=>
		@$state.go 'app.admin.domains.detail.list', {id: domain.id}

@controllers.controller 'AdminDomainsController', AdminDomainsController
