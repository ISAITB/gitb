package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}

import javax.inject.Inject
import managers.{AuthorizationManager, CommunityLabelManager, ConformanceManager, SpecificationManager}
import models.Enums.LabelType
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.JsonUtil

class SpecificationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, specificationManager: SpecificationManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SpecificationService])

  def deleteSpecification(specId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteSpecification(request, specId)
    conformanceManager.deleteSpecification(specId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateSpecification(specId: Long) = authorizedAction { request =>
    authorizationManager.canUpdateSpecification(request, specId)
    val specExists = specificationManager.checkSpecificationExists(specId)
    if (specExists) {
      val sname: String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
      val fname: String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
      val descr: Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
      val hidden = ParameterExtractor.requiredBodyParameter(request, Parameters.HIDDEN).toBoolean
      val groupId = ParameterExtractor.optionalLongBodyParameter(request, Parameters.GROUP_ID)
      specificationManager.updateSpecification(specId, sname, fname, descr, hidden, groupId)
      ResponseConstructor.constructEmptyResponse
    } else {
      throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, communityLabelManager.getLabel(request, LabelType.Specification) + " with ID '" + specId + "' not found.")
    }
  }

  def createSpecificationGroup() = authorizedAction { request =>
    val group = ParameterExtractor.extractSpecificationGroup(request)
    authorizationManager.canCreateSpecification(request, group.domain)
    specificationManager.createSpecificationGroup(group)
    ResponseConstructor.constructEmptyResponse
  }

  def updateSpecificationGroup(groupId: Long) = authorizedAction { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId)
    val shortname: String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
    val fullname: String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
    val description: Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
    specificationManager.updateSpecificationGroup(groupId, shortname, fullname, description)
    ResponseConstructor.constructEmptyResponse
  }

  def deleteSpecificationGroup(groupId: Long) = authorizedAction { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId)
    val withSpecifications = ParameterExtractor.optionalBooleanBodyParameter(request, Parameters.SPECS).getOrElse(false)
    specificationManager.deleteSpecificationGroup(groupId, withSpecifications)
    ResponseConstructor.constructEmptyResponse
  }

  def removeSpecificationFromGroup(specificationId: Long) = authorizedAction { request =>
    authorizationManager.canManageSpecification(request, specificationId)
    specificationManager.removeSpecificationFromGroup(specificationId)
    ResponseConstructor.constructEmptyResponse
  }

  def addSpecificationToGroup(groupId: Long) = authorizedAction { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId)
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPEC).toLong
    authorizationManager.canManageSpecification(request, specificationId)
    specificationManager.addSpecificationToGroup(specificationId, groupId)
    ResponseConstructor.constructEmptyResponse
  }

  def copySpecificationToGroup(groupId: Long) = authorizedAction { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId)
    val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPEC).toLong
    authorizationManager.canManageSpecification(request, specificationId)
    val newSpecificationId = specificationManager.copySpecificationToGroup(specificationId, groupId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsId(newSpecificationId).toString())
  }

  def getSpecificationGroups() = authorizedAction { request =>
    val domainId = ParameterExtractor.requiredQueryParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canManageDomain(request, domainId)
    val groups = specificationManager.getSpecificationGroups(domainId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroups(groups).toString())
  }

  def getSpecificationGroupsOfDomains() = authorizedAction { request =>
    val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
    authorizationManager.canViewDomains(request, domainIds)
    val groups = specificationManager.getSpecificationGroupsByDomainIds(domainIds)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroups(groups).toString())
  }

  def getSpecificationGroup(groupId: Long) = authorizedAction { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId)
    val group = specificationManager.getSpecificationGroupById(groupId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroup(group).toString())
  }

  def saveSpecificationOrder() = authorizedAction { request =>
    val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canManageDomain(request, domainId)
    val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS).getOrElse(List.empty)
    val groupOrders = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_ORDERS).getOrElse(List.empty)
    val specIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS).getOrElse(List.empty)
    val specOrders = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_ORDERS).getOrElse(List.empty)
    specificationManager.saveSpecificationOrder(groupIds, groupOrders, specIds, specOrders)
    ResponseConstructor.constructEmptyResponse
  }

  def resetSpecificationOrder() = authorizedAction { request =>
    val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canManageDomain(request, domainId)
    specificationManager.resetSpecificationOrder(domainId)
    ResponseConstructor.constructEmptyResponse

  }
}
