class SystemsController
	@$inject = ['$log', '$scope', '$state', '$stateParams', '$window', 'SystemService', 'ErrorService', '$uibModal', 'DataService', '$location']
	constructor: (@$log, @$scope, @$state, @$stateParams, @$window, @SystemService, @ErrorService, @$uibModal, @DataService, @$location) ->
		@$log.debug "Constructing SystemsController"
		@systems  = []       # systems of the organization
		@alerts   = []       # alerts to be displayed
		@modalAlerts   = []    # alerts to be displayed within modals
		@$scope.sdata  = {}    # bindings for new user
		@systemSpinner = false # spinner to be displayed for new system operations
		@organization = JSON.parse(@$window.localStorage['organization'])
		@$scope.editing = false
		@showAction = @DataService.isSystemAdmin || @DataService.isCommunityAdmin || (@DataService.isVendorAdmin && @DataService.community.allowPostTestSystemUpdates)

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
		@getSystems(true)

	getSystems: (initialLoad) ->
		checkIfHasTests = @DataService.isVendorAdmin && !@DataService.community.allowPostTestSystemUpdates
		@SystemService.getSystemsByOrganization(@organization.id, checkIfHasTests)
		.then(
			(data) =>
				@systems = data
				for system in @systems
					system.editable = false
					if @DataService.isSystemAdmin || @DataService.isCommunityAdmin
						system.editable = true
						@showAction = true
					else if @DataService.isVendorAdmin
						if @DataService.community.allowPostTestSystemUpdates
							system.editable = true
							@showAction = true
						else if !system.hasTests
							system.editable = true
							@showAction = true
						
				if initialLoad && @$stateParams['id']?
					systemToEdit = _.find @systems, (s) => 
						s.id == @$stateParams['id']
					if systemToEdit?
						@onSystemEdit(systemToEdit)
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
				communityId: () => @organization.community
				viewProperties: () => false
			size: 'lg'
		modalInstance = @$uibModal.open(modalOptions)
		modalInstance.result
			.finally(angular.noop)
			.then((result) => 
				if result?.ok
					@getSystems()
			, angular.noop)

	onSystemSelect: (system) =>
		if !@$scope.editing
			@$state.go 'app.systems.detail.conformance.list', { id : system.id }

	onSystemEdit: (system) =>
		if @isSystemEditable(system)
			@$scope.editing = true
			modalOptions =
				templateUrl: 'assets/views/systems/create-edit-system-modal.html'
				controller: 'CreateEditSystemController as systemCtrl'
				resolve: 
					system: () => system
					organisationId: () => @organization.id
					communityId: () => @organization.community
					viewProperties: () => @$stateParams['viewProperties']? && @$stateParams['viewProperties']
				size: 'lg'
			modalInstance = @$uibModal.open(modalOptions)
			modalInstance.result
				.finally(angular.noop)
				.then((result) => 
					@$scope.editing = false
					if result?.ok
						@getSystems()
				, () => 
					# Dismissed
					@$scope.editing = false
				)

	showBack: () =>
		@organization? && @DataService.vendor? && @organization.id != @DataService.vendor.id

	isSystemEditable: (system) =>
		system.editable

	showCreate: () =>
		@DataService.isSystemAdmin || @DataService.isCommunityAdmin || (@DataService.isVendorAdmin && @DataService.community.allowSystemManagement)

	redirect: (address, systemId) ->
		@$location.path(address + "/" + systemId)

	back: () ->
		@$state.go 'app.admin.users.communities.detail.organizations.detail.list', { community_id : JSON.parse(@$window.localStorage['community']).id, org_id : @organization.id }

@controllers.controller('SystemsController', SystemsController)