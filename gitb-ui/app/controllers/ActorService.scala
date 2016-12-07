package controllers

import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import managers.{ActorManager, OrganizationManager, UserManager}
import models.Enums.UserRole
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil
import exceptions.{InvalidAuthorizationException, ErrorCodes, NotFoundException}

import scala.concurrent.Future

class ActorService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ActorService])

  def deleteActor(actorId: Long) = Action.async {
    ActorManager.deleteActor(actorId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def updateActor(actorId: Long) = Action.async { request =>
    ActorManager.checkActorExists(actorId) map { actorExists =>
      if(actorExists) {
        val shortName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
        val fullName:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
        val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)

        ActorManager.updateActor(actorId, shortName, fullName, description)
        ResponseConstructor.constructEmptyResponse
      } else{
        throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "Actor with ID '" + actorId + "' not found")
      }
    }
  }
}
