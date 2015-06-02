class SystemConfigurationsController
	@$inject = ['$log', '$scope', '$stateParams', '$state', '$modal', 'SystemService', 'ErrorService']
	constructor: (@$log, @$scope, @$stateParams, @$state, @$modal, @SystemService, @ErrorService)->
		@$log.debug 'Constructing SystemConfigurationsController'
		@systemId = @$stateParams["id"]
		@configurations = []
		@alerts         = [] # alerts to be displayed
		@spinner        = false

		@$scope.onFileSelect = (event) =>
            file   = event.target.files[0];
            index  = angular.element(event.target).scope().$index;
            reader = new FileReader();
            reader.onload = (evt) =>
                content = evt.target.result;
                @configurations[index].value = content
            reader.readAsBinaryString(file);

		@getEndpointConfigurations()

	getEndpointConfigurations :()->
        @spinner = true  #start spinner
        @SystemService.getEndpointConfigurations(@systemId)
            .then(
                (data) =>
                    @configurations = data
                    @spinner =false  #stop spinner
                ,
                (error) =>
                    @ErrorService.showErrorMessage(error)
                    @spinner = false  #stop spinner
            )

    saveConfigurations :() ->
        @alerts  = []
        @spinner = true
        @SystemService.saveEndpointConfiguration(@systemId, @configurations)
            .then(
                (data) =>
                    @alerts.push({type:'success', msg:"System configurations updated."})
                    @spinner = false
                ,
                (error) =>
                    @ErrorService.showErrorMessage(error)
                    @spinner = false
            )

    closeAlert: (index) ->
        @alerts.splice(index, 1)

@controllers.controller 'SystemConfigurationsController', SystemConfigurationsController