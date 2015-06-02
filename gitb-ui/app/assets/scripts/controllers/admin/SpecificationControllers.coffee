class CreateSpecificationController
	name: 'CreateSpecificationController'

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams, @ErrorService) ->
		@$log.debug "Constructing #{@name}"

		@specification = {}

	createSpecification: ()=>
		if @specification.sname? and	@specification.sname?

			domainId = @$stateParams.id

			@ConformanceService.createSpecification @specification.sname, @specification.fname, @specification.urls, @specification.diagram, @specification.description, @specification.spec_type, domainId
			.then () =>
				@$state.go 'app.admin.domains.detail.list'
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

class SpecificationDetailsController
	name: 'SpecificationDetailsController'

	@$inject = ['$log', '$scope', 'ConformanceService', 'TestSuiteService', '$state', '$stateParams', '$modal', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @TestSuiteService, @$state, @$stateParams, @$modal, @ErrorService) ->
		@$log.debug "Constructing #{@name}"

		@specification = {}
		@actors = []
		@testSuites = []
		@domainId = @$stateParams.id
		@specificationId = @$stateParams.spec_id

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

		@testSuiteTableColumns = [
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
			{
				field: 'version',
				title: 'Version'
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

		@ConformanceService.getSpecificationsWithIds([@specificationId])
		.then (data) =>
			@specification = _.head data 
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getActorsWithSpecificationId(@specificationId)
		.then (data)=>
			@actors = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@ConformanceService.getTestSuites(@specificationId)
		.then (data)=>
			@testSuites = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onFileSelect: (files) =>
		@$log.debug "Test suite zip files is selected to deploy: ", files

		if files.length > 0
			@ConformanceService.deployTestSuite @specificationId, files[0]
			.progress (event) =>
				@$log.debug "File upload: " + (100.0 * event.loaded / event.total) + " percent uploaded."
			.success () =>
				@$state.go(@$state.$current, null, { reload: true });

	onTestSuiteDelete: (data) =>
		@$log.debug "Removing test suite ", data.id, data.sname
		@TestSuiteService.undeployTestSuite data.id
		.then () =>
			@$state.go(@$state.$current, null, { reload: true });
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	addExistingActors: () =>
		@ConformanceService.getActorsWithDomainId(@specification.domain)
		.then (data) =>
			domainActors = data
			domainActors = _.filter domainActors, (domainActor) =>
				not _.some @actors, (existingActor) =>
					domainActor.id == existingActor.id

			if domainActors.length > 0
				options =
					templateUrl: 'assets/views/admin/domains/add-existing-actors.html'
					controller: 'AddExistingActorsController as addExistingActorsCtrl'
					resolve: 
						specification: () =>	@specification
						existingActors: () => @actors
						domainActors: () => domainActors
					size: 'lg'

				instance = @$modal.open options
				instance.result
				.then (actors) =>
					if actors?
						_.forEach actors, (actor) =>
							@actors.push actor
				.catch () =>
					@$log.debug "An error occurred or the user dismissed the modal dialog"
			else
				@$log.debug "No additional actors that can be added exists in this domain."

		.catch (error) =>
			@ErrorService.showErrorMessage(error)

	onActorSelect: (actor) =>
		@$state.go 'app.admin.domains.detail.specifications.detail.actors.detail.list', {id: @domainId, spec_id: @specificationId, actor_id: actor.id}

class AddExistingActorsController
	name: 'AddExistingActorsController'
	@$inject = ['$scope', '$log', '$q', 'ConformanceService', '$modalInstance', 'specification', 'existingActors', 'domainActors', 'ErrorService']

	constructor: (@$scope, @$log, @$q, @ConformanceService, @$modalInstance, @specification, @existingActors, @domainActors, @ErrorService) ->
		@$log.debug "Constructing #{@name}"

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

		@selectedActors = []

	onActorSelect: (actor) =>
		@selectedActors.push actor

	onActorDeselect: (actor) =>
		_.remove @selectedActors, (a)->
			actor.id == a.id

	save: () =>
		promises = _.map @selectedActors, (actor) =>
			@ConformanceService.addActorToSpecification @specification.id, actor.id

		@$q.all promises
		.then ()=>
			@$modalInstance.close @selectedActors
		.catch (error)=>
			@$modalInstance.dismiss error

	cancel: () =>
		@$modalInstance.dismiss()

@ControllerUtils.register @controllers, AddExistingActorsController
@ControllerUtils.register @controllers, SpecificationDetailsController
@ControllerUtils.register @controllers, CreateSpecificationController