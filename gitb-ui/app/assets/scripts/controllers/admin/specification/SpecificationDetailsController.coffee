class SpecificationDetailsController

	@$inject = ['$log', '$scope', 'ConformanceService', 'TestSuiteService', 'ConfirmationDialogService', 'SpecificationService', '$state', '$stateParams', '$modal', 'ErrorService']
	constructor: (@$log, @$scope, @ConformanceService, @TestSuiteService, @ConfirmationDialogService, @SpecificationService, @$state, @$stateParams, @$modal, @ErrorService) ->
		@$log.debug "Constructing SpecificationDetailsController"

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

	deleteSpecification: () =>
		@ConfirmationDialogService.confirm("Confirm delete", "Are you sure you want to delete this specification?", "Yes", "No")
		.then () =>
			@SpecificationService.deleteSpecification(@specificationId)
			.then () =>
				@$state.go 'app.admin.domains.detail.list', {id: @domainId}
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

	saveSpecificationChanges: () =>
		@SpecificationService.updateSpecification(@specificationId, @specification.sname, @specification.fname, @specification.urls, @specification. diagram, @specification.description, @specification.spec_type)
		.then () =>
			@$state.go 'app.admin.domains.detail.list', {id: @domainId}
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

@controllers.controller 'SpecificationDetailsController', SpecificationDetailsController
