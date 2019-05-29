package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}
import javax.inject.Inject
import managers.{AuthorizationManager, ConformanceManager, SpecificationManager}
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Controller

class SpecificationService @Inject() (specificationManager: SpecificationManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SpecificationService])

  def deleteSpecification(specId: Long) = AuthorizedAction { request =>
    authorizationManager.canDeleteSpecification(request, specId)
    conformanceManager.deleteSpecification(specId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateSpecification(specId: Long) = AuthorizedAction { request =>
    authorizationManager.canUpdateSpecification(request, specId)
    val specExists = specificationManager.checkSpecifiationExists(specId)
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

      specificationManager.updateSpecification(specId, sname, fname, urls, diagram, descr, specificationType)
      ResponseConstructor.constructEmptyResponse
    } else{
      throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, "Specification with ID '" + specId + "' not found")
    }
  }

}
