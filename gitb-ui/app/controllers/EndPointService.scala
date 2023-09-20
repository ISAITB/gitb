package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import javax.inject.Inject
import managers.{AuthorizationManager, CommunityLabelManager, EndPointManager}
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{AbstractController, ControllerComponents}

class EndPointService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, endPointManager: EndPointManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[EndPointService])

  def deleteEndPoint(endPointId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteEndpoint(request, endPointId)
    endPointManager.deleteEndPoint(endPointId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateEndPoint(endPointId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateEndpoint(request, endPointId)
    val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    if (endPointManager.checkEndPointExistsForActor(name, actorId, Some(endPointId))) {
      val labels = communityLabelManager.getLabelsByUserId(ParameterExtractor.extractUserId(request))
      ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, models.Enums.LabelType.Endpoint) + " with this name already exists for the " + communityLabelManager.getLabel(labels, models.Enums.LabelType.Actor, true, true)+".")
    } else{
      endPointManager.updateEndPointWrapper(endPointId, name, description)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
