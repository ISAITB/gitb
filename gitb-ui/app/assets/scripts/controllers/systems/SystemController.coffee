class SystemController
	@$inject = ['$stateParams', 'SystemService', 'ErrorService', 'DataService']
	constructor:(@$stateParams, @SystemService, @ErrorService, @DataService) ->
		@systemId = @$stateParams["id"]
		@propertyData = {
			properties: []
			edit: @$stateParams["viewProperties"]? && @$stateParams["viewProperties"]
		}
		@SystemService.getSystem(@systemId)
		.then (data) =>
			@system = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

		@SystemService.getSystemParameterValues(@systemId)
		.then (data) =>
			@propertyData.properties = data
		.catch (error) =>
			@ErrorService.showErrorMessage(error)

controllers.controller('SystemController', SystemController)