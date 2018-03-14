class ActorService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

  deleteActor: (actorId) ->
    @RestService.delete
      path: jsRoutes.controllers.ActorService.deleteActor(actorId).url
      authenticate: true

  updateActor: (id, actorId, name, description, domainId, specificationId) ->
    @RestService.post({
      path: jsRoutes.controllers.ActorService.updateActor(id).url,
      data: {
        actor_id: actorId
        name: name
        description: description
        domain_id: domainId
        spec_id: specificationId
      }
      authenticate: true
    })

services.service('ActorService', ActorService)
