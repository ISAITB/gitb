package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, ResponseConstructor}
import javax.inject.Inject
import managers.{AuthorizationManager, ParameterManager}
import play.api.mvc.Controller

class ParameterService @Inject() (parameterManager: ParameterManager, authorizationManager: AuthorizationManager) extends Controller {

  def deleteParameter(parameterId: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteParameter(request, parameterId)
    parameterManager.deleteParameter(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateParameter(parameterId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateParameter(request, parameterId)
    val parameter = ParameterExtractor.extractParameter(request)
    if (parameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, Some(parameterId))) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the endpoint")
    } else{
      parameterManager.updateParameterWrapper(parameterId, parameter.name, parameter.desc, parameter.use, parameter.kind)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
