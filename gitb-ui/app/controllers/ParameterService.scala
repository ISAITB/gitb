package controllers

import controllers.util.{ParameterExtractor, ResponseConstructor}
import javax.inject.Inject
import managers.ParameterManager
import play.api.mvc.{Action, Controller}

class ParameterService @Inject() (parameterManager: ParameterManager) extends Controller {

  def deleteParameter(parameterId: Long) = Action.apply { request =>
    parameterManager.deleteParameter(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateParameter(parameterId: Long) = Action.apply { request =>
    val parameter = ParameterExtractor.extractParameter(request)
    if (parameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, Some(parameterId))) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the endpoint")
    } else{
      parameterManager.updateParameterWrapper(parameterId, parameter.name, parameter.desc, parameter.use, parameter.kind)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
