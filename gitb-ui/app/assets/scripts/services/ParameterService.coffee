class ParameterService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  deleteParameter: (parameterId) ->
    @RestService.delete
      path: jsRoutes.controllers.ParameterService.deleteParameter(parameterId).url
      authenticate: true

  updateParameter: (parameterId, name, description, use, kind, adminOnly, notForTests, hidden, endpointId) ->
    @RestService.post({
      path: jsRoutes.controllers.ParameterService.updateParameter(parameterId).url,
      data: {
        name: name
        description: description
        use: use
        kind: kind
        admin_only: adminOnly
        not_for_tests: notForTests
        hidden: hidden
        endpoint_id: endpointId
      }
      authenticate: true
    })

services.service('ParameterService', ParameterService)