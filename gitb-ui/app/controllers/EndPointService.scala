package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}
import managers.EndPointManager
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{Action, Controller}

class EndPointService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[EndPointService])

  def deleteEndPoint(endPointId: Long) = Action.apply { request =>
    EndPointManager.deleteEndPoint(endPointId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateEndPoint(endPointId: Long) = Action.apply { request =>
    val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    if (EndPointManager.checkEndPointExistsForActor(name, actorId, Some(endPointId))) {
      ResponseConstructor.constructBadRequestResponse(500, "An endpoint with this name already exists for the actor")
    } else{
      EndPointManager.updateEndPoint(endPointId, name, description)
      ResponseConstructor.constructEmptyResponse
    }
  }

}
