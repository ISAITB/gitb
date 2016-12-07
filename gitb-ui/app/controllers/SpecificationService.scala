package controllers

import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import managers.{SpecificationManager, ActorManager, OrganizationManager, UserManager}
import models.Enums.UserRole
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil
import exceptions.{InvalidAuthorizationException, ErrorCodes, NotFoundException}

import scala.concurrent.Future

class SpecificationService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SpecificationService])

  def deleteSpecification(specId: Long) = Action.async {
    SpecificationManager.deleteSpecification(specId) map { unit =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def updateSpecification(specId: Long) = Action.async { request =>
    SpecificationManager.checkSpecifiationExists(specId) map { specExists =>
      if(specExists) {
        val sname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
        val fname:String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
        val urls = ParameterExtractor.optionalBodyParameter(request, Parameters.URLS)
        val diagram:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DIAGRAM)
        val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
        val specificationType: Option[Short] = ParameterExtractor.optionalBodyParameter(request, Parameters.SPEC_TYPE) match {
          case Some(str) => Some(str.toShort)
          case _ => None
        }

        SpecificationManager.updateSpecification(specId, sname, fname, urls, diagram, descr, specificationType)
        ResponseConstructor.constructEmptyResponse
      } else{
        throw new NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "Specification with ID '" + specId + "' not found")
      }
    }
  }

}
