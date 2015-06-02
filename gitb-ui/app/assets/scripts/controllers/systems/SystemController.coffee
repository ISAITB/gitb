class SystemController
	constructor:(@$log, @$location, @$scope, @$stateParams, @DataService, @SystemService, @ErrorService, @Constants) ->
		@$log.debug "Constructing SystemController"

		@ds = @DataService #shorten service name
		@alerts       = [] # alerts to be displayed
		@selectedTab  = 1  # selected tab
		@$scope.sdata = {} # bindings for system
		@$scope.systemId = @$stateParams["id"]
		@spinner  = false

		#initially get the system with given id
		@getSystemProfile(@$scope.systemId)

	getSystemProfile: (systemId) ->
		@spinner = true  #start spinner
		@SystemService.getSystem(systemId)
		.then(
			(data) =>
				@system = data
				@$scope.sdata.sname   = @system.sname
				@$scope.sdata.fname   = @system.fname
				@$scope.sdata.version = @system.version
				@$scope.sdata.description = @system.description
				#stop spinner
				@spinner = false
			,
			(error) =>
				@ErrorService.showErrorMessage(error)
				#stop spinner
				@spinner = false
		)

	updateSystemProfile: () ->
		if @checkForm()
			@spinner = true  #start spinner
			@SystemService.updateSystem(@$scope.systemId, @$scope.sdata.sname, @$scope.sdata.fname,
										@$scope.sdata.description, @$scope.sdata.version)
			.then(
				(data) =>
					@alerts.push({type:'success', msg:"System information updated."})
					@system.sname = @$scope.sdata.sname
					@system.fname = @$scope.sdata.fname
					@system.description = @$scope.sdata.description
					@system.version = @$scope.sdata.version
					#stop spinner
					@spinner = false
				,
				(error) =>
					@ErrorService.showErrorMessage(error)
					#stop spinner
					@spinner = false
					#revert back the changes
					@revertChanges()
			)
		else #revert back the changes
			@revertChanges()

	checkForm: () ->
		@alerts = [] # remove all the alerts
		valid = true

		if @$scope.sdata.fname == undefined || @$scope.sdata.fname == ''
			@alerts.push({type:'danger', msg:"Full name of your system can not be empty."})
			valid = false
		else if @$scope.sdata.sname == undefined || @$scope.sdata.sname == ''
			@alerts.push({type:'danger', msg:"Short name of your system can not be empty."})
			valid = false
		else if @$scope.sdata.version == undefined || @$scope.sdata.version == ''
			@alerts.push({type:'danger', msg:"Version of your system can not be empty."})
			valid = false

		valid

	revertChanges: () ->
		@$scope.sdata.fname = @system.fname
		@$scope.sdata.sname = @system.sname
		@$scope.sdata.description = @system.description
		@$scope.sdata.version = @system.version

	closeAlert: (index) ->
		@alerts.splice(index, 1)

controllers.controller('SystemController', SystemController)