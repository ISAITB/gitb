package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, ResponseConstructor}
import javax.inject.Inject
import managers.{AuthorizationManager, CommunityLabelManager, ParameterManager}
import play.api.mvc.{AbstractController, ControllerComponents}

class ParameterService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, parameterManager: ParameterManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {

  def deleteParameter(parameterId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteParameter(request, parameterId)
    parameterManager.deleteParameter(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateParameter(parameterId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateParameter(request, parameterId)
    val parameter = ParameterExtractor.extractParameter(request)
    if (parameterManager.checkParameterExistsForEndpoint(parameter.name, parameter.endpoint, Some(parameterId))) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this name already exists for the " + communityLabelManager.getLabel(request, models.Enums.LabelType.Endpoint, true, true)+".")
    } else{
      parameterManager.updateParameterWrapper(parameterId, parameter.name, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
