class AddExistingActorsController
	@$inject = ['$scope', '$log', '$q', 'ConformanceService', '$modalInstance', 'specification', 'existingActors', 'domainActors', 'ErrorService']

	constructor: (@$scope, @$log, @$q, @ConformanceService, @$modalInstance, @specification, @existingActors, @domainActors, @ErrorService) ->
		@$log.debug "Constructing AddExistingActorsController"

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

@controllers.controller 'AddExistingActorsController', AddExistingActorsController
