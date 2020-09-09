class TriggerService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  getTriggersByCommunity: (communityId) ->
    @RestService.get({
      path: jsRoutes.controllers.TriggerService.getTriggersByCommunity(communityId).url,
      authenticate: true
    })

  createTrigger: (name, description, operation, active, url, event, communityId, dataItems) ->
    data = {
        name: name,
        description: description,
        url: url,
        active: active? && active
        operation: operation,
        event: event,
        community_id: communityId
    }
    if dataItems?
      data.data = JSON.stringify(dataItems)
    @RestService.post({
      path: jsRoutes.controllers.TriggerService.createTrigger().url,
      authenticate: true,
      data: data
    })

  preview: (operation, dataItems, communityId) ->
    data = {
        operation: operation,
        community_id: communityId
    }
    if dataItems?
      data.data = JSON.stringify(dataItems)
    @RestService.post({
      path: jsRoutes.controllers.TriggerService.previewTriggerCall().url,
      authenticate: true,
      data: data
    })

  getTriggerById: (triggerId) ->
    @RestService.get({
      path: jsRoutes.controllers.TriggerService.getTriggerById(triggerId).url,
      authenticate: true
    })

  updateTrigger: (triggerId, name, description, operation, active, url, event, communityId, dataItems) ->
    data = {
        name: name,
        description: description,
        url: url,
        active: active? && active,
        operation: operation,
        event: event,
        community_id: communityId
    }
    if dataItems?
      data.data = JSON.stringify(dataItems)
    @RestService.post({
      path: jsRoutes.controllers.TriggerService.updateTrigger(triggerId).url,
      authenticate: true,
      data: data
    })

  deleteTrigger: (triggerId) ->
    @RestService.delete
      path: jsRoutes.controllers.TriggerService.deleteTrigger(triggerId).url,
      authenticate: true

  clearStatus: (triggerId) ->
    @RestService.post({
      path: jsRoutes.controllers.TriggerService.clearStatus(triggerId).url,
      authenticate: true
    })

  testTriggerEndpoint: (url, communityId) ->
    @RestService.post
      path: jsRoutes.controllers.TriggerService.testTriggerEndpoint().url,
      authenticate: true,
      data: {
        url: url,
        community_id: communityId
      }

services.service('TriggerService', TriggerService)