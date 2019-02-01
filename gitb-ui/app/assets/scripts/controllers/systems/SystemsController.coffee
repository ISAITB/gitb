class SystemsController
	@$inject = ['$log', '$scope', '$state', '$window', 'SystemService', 'ErrorService', '$uibModal', 'DataService', '$location']
	constructor: (@$log, @$scope, @$state, @$window, @SystemService, @ErrorService, @$uibModal, @DataService, @$location) ->
		@$log.debug "Constructing SystemsController"
		@systems  = []       # systems of the organization
		@alerts   = []       # alerts to be displayed
		@modalAlerts   = []    # alerts to be displayed within modals
		@$scope.sdata  = {}    # bindings for new user
		@systemSpinner = false # spinner to be displayed for new system operations
		@organization = JSON.parse(@$window.localStorage['organization'])
		@$scope.editing = false
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
			{
				field: 'version',
				title: 'Version'
			}
		]
		#initially get the registered Systems of the vendor
		@getSystems()

	getSystems: () ->
		@SystemService.getSystemsByOrganization(@organization.id)
		.then(
			(data) =>
				@systems = data
			,
			(error) =>
				@ErrorService.showErrorMessage(error)
		)

	createSystem: () =>
		modalOptions =
			templateUrl: 'assets/views/systems/create-edit-system-modal.html'
			controller: 'CreateEditSystemController as systemCtrl'
			resolve: 
				system: () => {}
				organisationId: () => @organization.id
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.then((result) => 
				@systems = result
			)

	onSystemSelect: (system) =>
		if !@$scope.editing
			@$state.go 'app.systems.detail.conformance.list', { id : system.id }

	onSystemEdit: (system) =>
		@$scope.editing = true
		modalOptions =
			templateUrl: 'assets/views/systems/create-edit-system-modal.html'
			controller: 'CreateEditSystemController as systemCtrl'
			resolve: 
				system: () => system
				organisationId: () => @organization.id
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.then((result) => 
				@$scope.editing = false
				@systems = result
			, () => 
				# Dismissed
				@$scope.editing = false
			)

	showBack: () =>
		@organization? && @DataService.vendor? && @organization.id != @DataService.vendor.id

	showAction: () =>
		!@DataService.isVendorUser

	showCreate: () =>
		!@DataService.isVendorUser

	redirect: (address, systemId) ->
		@$location.path(address + "/" + systemId)

	back: () ->
		@$state.go 'app.admin.users.communities.detail.organizations.detail.list', { community_id : JSON.parse(@$window.localStorage['community']).id, org_id : @organization.id }

@controllers.controller('SystemsController', SystemsController)