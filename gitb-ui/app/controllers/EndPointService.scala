package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import javax.inject.Inject
import managers.{AuthorizationManager, EndPointManager}
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Controller

class EndPointService @Inject() (endPointManager: EndPointManager, authorizationManager: AuthorizationManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[EndPointService])

  def deleteEndPoint(endPointId: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteEndpoint(request, endPointId)
    endPointManager.deleteEndPoint(endPointId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateEndPoint(endPointId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateEndpoint(request, endPointId)
    val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    if (endPointManager.checkEndPointExistsForActor(name, actorId, Some(endPointId))) {
      ResponseConstructor.constructBadRequestResponse(500, "An endpoint with this name already exists for the actor")
    } else{
      endPointManager.updateEndPointWrapper(endPointId, name, description)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
