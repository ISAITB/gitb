class SpecificationService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  deleteSpecification: (specId) ->
    @RestService.delete
      path: jsRoutes.controllers.SpecificationService.deleteSpecification(specId).url
      authenticate: true

  updateSpecification: (specId, shortName, fullName, description, hidden) ->
    if hidden == undefined
      hidden = false
    @RestService.post({
      path: jsRoutes.controllers.SpecificationService.updateSpecification(specId).url,
      data: {
        sname: shortName
        fname: fullName
        description: description
        hidden: hidden
      }
      authenticate: true
    })

services.service('SpecificationService', SpecificationService)
