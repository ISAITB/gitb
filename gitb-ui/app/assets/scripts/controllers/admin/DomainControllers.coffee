class AdminDomainsController
	name: 'AdminDomainsController'

	@$inject = ['$log', '$scope', '$state', 'ConformanceService', 'ErrorService']
	constructor: (@$log, @$scope, @$state, @ConformanceService, @ErrorService) ->
		@$log.debug "Constructing #{@name}..."

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

class DomainDetailsController
	name: 'DomainDetailsController'

	@$inject = ['$log', '$scope', '$state', '$stateParams', 'ConfirmationDialogService', 'ConformanceService', 'ErrorService']
	constructor: (@$log, @$scope, @$state, @$stateParams, @ConfirmationDialogService, @ConformanceService, @ErrorService) ->
		@$log.debug "Constructing #{@name}..."

		@domain = {}
		@specifications = []
		@actors = []
		@domainId = @$stateParams.id

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

		@actorTableColumns = [
			{
				field: 'actorId',
				title: 'ID'
			}
			{
				field: 'name',
				title: 'Name'
			}
			{
				field: 'description',
				title: 'Description'
			}
		]

		@ConformanceService.getDomains([@domainId])
		.then (data) =>
			@domain = _.head data 
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getSpecifications(@domainId)
		.then (data)=>
			@specifications = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getActorsWithDomainId(@domainId)
		.then (data)=>
			@actors = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	deleteDomain: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this domain?", "Yes", "No")
		.then () =>
			@ConformanceService.deleteDomain(@domainId)
			.then () =>
				@$state.go 'app.admin.domains.list'
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveDomainChanges: () =>
		@ConformanceService.updateDomain(@domainId, @domain.sname, @domain.fname, @domain.description)
		.then () =>
			@$state.go 'app.admin.domains.list'
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onSpecificationSelect: (specification) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.list', {id: @domainId, spec_id: specification.id}

	onActorSelect: (actor) =>
		@$state.go 'app.admin.domains.detail.actors.detail.list', {id: @domainId, actor_id: actor.id}

@ControllerUtils.register @controllers, DomainDetailsController
@ControllerUtils.register @controllers, AdminDomainsController
@ControllerUtils.register @controllers, CreateDomainController
