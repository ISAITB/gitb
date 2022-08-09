package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, ResponseConstructor}
import javax.inject.Inject
import managers.{AuthorizationManager, CommunityLabelManager, ParameterManager}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

class ParameterService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, parameterManager: ParameterManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {

  def deleteParameter(parameterId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteParameter(request, parameterId)
    parameterManager.deleteParameter(parameterId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateParameter(parameterId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateParameter(request, parameterId)
    val parameter = ParameterExtractor.extractParameter(request)
    if (parameterManager.checkParameterExistsForEndpoint(parameter.testKey, parameter.endpoint, Some(parameterId))) {
      ResponseConstructor.constructBadRequestResponse(500, "A parameter with this key already exists for the " + communityLabelManager.getLabel(request, models.Enums.LabelType.Endpoint, single = true, lowercase = true)+".")
    } else{
      parameterManager.updateParameterWrapper(parameterId, parameter.name, parameter.testKey, parameter.desc, parameter.use, parameter.kind, parameter.adminOnly, parameter.notForTests, parameter.hidden, parameter.allowedValues, parameter.dependsOn, parameter.dependsOnValue)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def orderParameters(endpointId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canUpdateEndpoint(request, endpointId)
    val orderedIds = ParameterExtractor.extractLongIdsBodyParameter(request)
    parameterManager.orderParameters(endpointId, orderedIds.getOrElse(List[Long]()))
    ResponseConstructor.constructEmptyResponse
  }

}
