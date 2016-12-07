package controllers

import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import managers.{EndPointManager, ActorManager, OrganizationManager, UserManager}
import models.Enums.UserRole
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil
import exceptions.{InvalidAuthorizationException, ErrorCodes, NotFoundException}

import scala.concurrent.Future

class EndPointService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[EndPointService])

  def deleteEndPoint(endPointId: Long) = Action.async {
    EndPointManager.deleteEndPoint(endPointId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def updateEndPoint(endPointId: Long) = Action.async { request =>
    EndPointManager.checkEndPointExists(endPointId) map { endPointExists =>
      if(endPointExists) {
        val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
        val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)

        EndPointManager.updateEndPoint(endPointId, name, description)
        ResponseConstructor.constructEmptyResponse
      } else{
        throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "Endpoint with ID '" + endPointId + "' not found")
      }
    }
  }

}
