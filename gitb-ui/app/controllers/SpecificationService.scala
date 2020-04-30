package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}
import javax.inject.Inject
import managers.{AuthorizationManager, CommunityLabelManager, ConformanceManager, SpecificationManager}
import models.Enums.LabelType
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Controller

class SpecificationService @Inject() (specificationManager: SpecificationManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends Controller {
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
      val descr:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
      val hidden = ParameterExtractor.requiredBodyParameter(request, Parameters.HIDDEN).toBoolean

      specificationManager.updateSpecification(specId, sname, fname, descr, hidden)
      ResponseConstructor.constructEmptyResponse
    } else{
      throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, communityLabelManager.getLabel(request, LabelType.Specification) + " with ID '" + specId + "' not found.")
    }
  }

}
