class SystemNavigationController

	@$inject = ['$log', '$scope', '$stateParams', '$state', 'SystemService', 'DataService', 'ErrorService']
	constructor: (@$log, @$scope, @$stateParams, @$state, @SystemService, @DataService, @ErrorService)->

		@count = 0

		if @DataService.isVendorUser
			@SystemService.getSystemsByOrganization(@DataService.vendor.id)
			.then (data) =>
				@count = data.length
			.catch (error) =>
				@ErrorService.showErrorMessage(error)

@controllers.controller 'SystemNavigationController', SystemNavigationController