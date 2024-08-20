package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.{ErrorCodes, NotFoundException}
import managers.{AuthorizationManager, CommunityLabelManager, SpecificationManager}
import models.BadgeInfo
import models.Enums.{LabelType, TestResultStatus}
import org.apache.commons.io.FileUtils
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{JsonUtil, RepositoryUtils}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SpecificationService @Inject() (implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, repositoryUtils: RepositoryUtils, specificationManager: SpecificationManager, authorizationManager: AuthorizationManager, communityLabelManager: CommunityLabelManager) extends AbstractController(cc) {

  def deleteSpecification(specId: Long) = authorizedAction { request =>
    authorizationManager.canDeleteSpecification(request, specId)
    specificationManager.deleteSpecification(specId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateSpecification(specId: Long) = authorizedAction { request =>
    try {
      authorizationManager.canUpdateSpecification(request, specId)
      val paramMap = ParameterExtractor.paramMap(request)
      val specExists = specificationManager.checkSpecificationExists(specId)
      if (specExists) {
        val badgeInfo = ParameterExtractor.extractBadges(request, paramMap, forReport = false)
        if (badgeInfo._2.nonEmpty) {
          badgeInfo._2.get
        } else {
          val badgeInfoForReport = ParameterExtractor.extractBadges(request, paramMap, forReport = true)
          if (badgeInfoForReport._2.nonEmpty) {
            badgeInfoForReport._2.get
          } else {
            val sname: String = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SHORT_NAME)
            val fname: String = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.FULL_NAME)
            val descr: Option[String] = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.DESC)
            val hidden = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.HIDDEN).toBoolean
            val groupId = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.GROUP_ID)
            specificationManager.updateSpecification(specId, sname, fname, descr, hidden, groupId, BadgeInfo(badgeInfo._1.get, badgeInfoForReport._1.get))
            ResponseConstructor.constructEmptyResponse
          }
        }
      } else {
        throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, communityLabelManager.getLabel(request, LabelType.Specification) + " with ID '" + specId + "' not found.")
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
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

  def getSpecificationOfActor(actorId: Long) = authorizedAction { request =>
    authorizationManager.canViewActor(request, actorId)
    val specification = specificationManager.getSpecificationOfActor(actorId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecification(specification).toString())
  }

  def getSpecificationIdOfActor(actorId: Long) = authorizedAction { request =>
    authorizationManager.canViewActor(request, actorId)
    val specificationId = specificationManager.getSpecificationIdOfActor(actorId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsId(specificationId).toString())
  }

  def getSpecificationGroups() = authorizedAction { request =>
    val domainId = ParameterExtractor.requiredQueryParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canViewDomains(request, Some(List(domainId)))
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
    ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroup(group, withApiKeys = true).toString())
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

  def getBadgeForStatus(specId: Long, status: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageSpecification(request, specId)
    val forReport = ParameterExtractor.optionalBooleanQueryParameter(request, Parameters.REPORT)
    val statusToLookup = TestResultStatus.withName(status).toString
    val badge = repositoryUtils.getConformanceBadge(specId, None, None, statusToLookup, exactMatch = true, forReport.getOrElse(false))
    if (badge.isDefined && badge.get.exists()) {
      Ok.sendFile(content = badge.get)
    } else {
      NotFound
    }
  }

}
