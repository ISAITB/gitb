class SpecificationService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  @$inject = ['$log', 'RestService']
  constructor: (@$log, @RestService) ->

  deleteSpecification: (specId) ->
    @RestService.delete
      path: jsRoutes.controllers.SpecificationService.deleteSpecification(specId).url
      authenticate: true

  updateSpecification: (specId, shortName, fullName, urls, diagram, description, specType) ->
    @RestService.post({
      path: jsRoutes.controllers.SpecificationService.updateSpecification(specId).url,
      data: {
        sname: shortName
        fname: fullName
        urls: urls
        diagram: diagram
        description: description
        spec_type: specType
      }
      authenticate: true
    })

services.service('SpecificationService', SpecificationService)
