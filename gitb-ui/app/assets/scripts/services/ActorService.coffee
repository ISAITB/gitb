class ActorService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  deleteActor: (actorId) ->
    @RestService.delete
      path: jsRoutes.controllers.ActorService.deleteActor(actorId).url
      authenticate: true

  updateActor: (id, actorId, name, description, defaultActor, displayOrder, domainId, specificationId) =>
    data = {
      actor_id: actorId
      name: name
      description: description
      default: defaultActor
      domain_id: domainId
      spec_id: specificationId
    }
    if displayOrder? && (!displayOrder.trim? || displayOrder.trim() != '')
      data.displayOrder = Number(displayOrder)
    @RestService.post({
      path: jsRoutes.controllers.ActorService.updateActor(id).url,
      authenticate: true
      data: data
    })

services.service('ActorService', ActorService)
