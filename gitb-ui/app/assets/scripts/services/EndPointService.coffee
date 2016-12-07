class EndPointService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

  deleteEndPoint: (endPointId) ->
    @RestService.delete
      path: jsRoutes.controllers.EndPointService.deleteEndPoint(endPointId).url
      authenticate: true

  updateEndPoint: (endPointId, name, description) ->
    @RestService.post({
      path: jsRoutes.controllers.EndPointService.updateEndPoint(endPointId).url,
      data: {
        name: name
        description: description
      }
      authenticate: true
    })

services.service('EndPointService', EndPointService)