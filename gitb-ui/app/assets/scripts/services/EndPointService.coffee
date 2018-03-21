class EndPointService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  deleteEndPoint: (endPointId) ->
    @RestService.delete
      path: jsRoutes.controllers.EndPointService.deleteEndPoint(endPointId).url
      authenticate: true

  updateEndPoint: (endPointId, name, description, actorId) ->
    @RestService.post({
      path: jsRoutes.controllers.EndPointService.updateEndPoint(endPointId).url,
      data: {
        name: name
        description: description
        actor_id: actorId
      }
      authenticate: true
    })

services.service('EndPointService', EndPointService)