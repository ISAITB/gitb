class ActorService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @RestService) ->

  deleteActor: (actorId) ->
    @RestService.delete
      path: jsRoutes.controllers.ActorService.deleteActor(actorId).url
      authenticate: true

  updateActor: (domainId, shortName, fullName, description) ->
    @RestService.post({
      path: jsRoutes.controllers.ActorService.updateActor(domainId).url,
      data: {
        sname: shortName
        fname: fullName
        description: description
      }
      authenticate: true
    })

services.service('ActorService', ActorService)
