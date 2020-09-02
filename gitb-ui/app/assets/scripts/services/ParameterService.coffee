class ParameterService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  deleteParameter: (parameterId) ->
    @RestService.delete
      path: jsRoutes.controllers.ParameterService.deleteParameter(parameterId).url
      authenticate: true

  updateParameter: (parameterId, name, description, use, kind, adminOnly, notForTests, hidden, allowedValues, dependsOn, dependsOnValue, endpointId) ->
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
        allowedValues: allowedValues
        dependsOn: dependsOn
        dependsOnValue: dependsOnValue
        endpoint_id: endpointId
      }
      authenticate: true
    })

  orderParameters: (endpointId, orderedIds) ->
    @RestService.post({
      path: jsRoutes.controllers.ParameterService.orderParameters(endpointId).url,
      data: {
        ids: orderedIds.join ','
      }
      authenticate: true
    })

services.service('ParameterService', ParameterService)