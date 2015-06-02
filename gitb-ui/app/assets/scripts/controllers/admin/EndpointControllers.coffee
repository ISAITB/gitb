class EndpointDetailsController
	name: 'EndpointDetailsController'

	@$inject = ['$log', '$scope', 'ConformanceService', '$state', '$stateParams']
	constructor: (@$log, @$scope, @ConformanceService, @$state, @$stateParams) ->
		@$log.debug "Constructing #{@name}"
		@endpointId = @$stateParams.endpoint_id

		@endpoint = null

		@parameterTableColumns = [
			{
				field: 'name'
				title: 'Name'
			}
			{
				field: 'desc'
				title: 'Description'
			}
			{
				field: 'use'
				title: 'Usage'
			}
			{
				field: 'kind'
				title: 'Type'
			}
		]

		@ConformanceService.getEndpoints [@endpointId]
		.then (data) =>
			@endpoint = _.head data

@ControllerUtils.register @controllers, EndpointDetailsController
