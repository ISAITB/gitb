package controllers

import controllers.util.{ParameterExtractor, ResponseConstructor}
import managers.ParameterManager
import play.api.mvc.{Action, Controller}

class ParameterService extends Controller {

  def deleteParameter(parameterId: Long) = Action.apply { request =>
    ParameterManager.deleteParameter(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateParameter(parameterId: Long) = Action.apply { request =>
    val parameter = ParameterExtractor.extractParameter(request)
    if (ParameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, Some(parameterId))) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the endpoint")
    } else{
      ParameterManager.updateParameterWrapper(parameterId, parameter.name, parameter.desc, parameter.use, parameter.kind)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
