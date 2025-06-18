/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
import scala.concurrent.{ExecutionContext, Future}

class SpecificationService @Inject() (authorizedAction: AuthorizedAction,
                                      cc: ControllerComponents,
                                      repositoryUtils: RepositoryUtils,
                                      specificationManager: SpecificationManager,
                                      authorizationManager: AuthorizationManager,
                                      communityLabelManager: CommunityLabelManager)
                                     (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def deleteSpecification(specId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteSpecification(request, specId).flatMap { _ =>
      specificationManager.deleteSpecification(specId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateSpecification(specId: Long): Action[AnyContent] = authorizedAction.async { request =>
    val task = for {
      _ <- authorizationManager.canUpdateSpecification(request, specId)
      result <- {
        val paramMap = ParameterExtractor.paramMap(request)
        specificationManager.checkSpecificationExists(specId).flatMap { specExists =>
          if (specExists) {
            val badgeInfo = ParameterExtractor.extractBadges(request, paramMap, forReport = false)
            if (badgeInfo._2.nonEmpty) {
              Future.successful(badgeInfo._2.get)
            } else {
              val badgeInfoForReport = ParameterExtractor.extractBadges(request, paramMap, forReport = true)
              if (badgeInfoForReport._2.nonEmpty) {
                Future.successful(badgeInfoForReport._2.get)
              } else {
                val sname: String = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.SHORT_NAME)
                val fname: String = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.FULL_NAME)
                val descr: Option[String] = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.DESC)
                val reportMetadata: Option[String] = ParameterExtractor.optionalBodyParameter(paramMap, Parameters.METADATA)
                val hidden = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.HIDDEN).toBoolean
                val groupId = ParameterExtractor.optionalLongBodyParameter(paramMap, Parameters.GROUP_ID)
                specificationManager.updateSpecification(specId, sname, fname, descr, reportMetadata, hidden, groupId, BadgeInfo(badgeInfo._1.get, badgeInfoForReport._1.get)).map { _ =>
                  ResponseConstructor.constructEmptyResponse
                }
              }
            }
          } else {
            communityLabelManager.getLabel(request, LabelType.Specification).map { specificationLabel =>
              throw NotFoundException(ErrorCodes.SYSTEM_NOT_FOUND, specificationLabel + " with ID '" + specId + "' not found.")
            }
          }
        }
      }
    } yield result
    task.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def createSpecificationGroup(): Action[AnyContent] = authorizedAction.async { request =>
    val group = ParameterExtractor.extractSpecificationGroup(request)
    authorizationManager.canCreateSpecification(request, group.domain).flatMap { _ =>
      specificationManager.createSpecificationGroup(group).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateSpecificationGroup(groupId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId).flatMap { _ =>
      val shortname: String = ParameterExtractor.requiredBodyParameter(request, Parameters.SHORT_NAME)
      val fullname: String = ParameterExtractor.requiredBodyParameter(request, Parameters.FULL_NAME)
      val description: Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESC)
      val reportMetadata: Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.METADATA)
      specificationManager.updateSpecificationGroup(groupId, shortname, fullname, description, reportMetadata).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def deleteSpecificationGroup(groupId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId).flatMap { _ =>
      val withSpecifications = ParameterExtractor.optionalBooleanBodyParameter(request, Parameters.SPECS).getOrElse(false)
      specificationManager.deleteSpecificationGroup(groupId, withSpecifications).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def removeSpecificationFromGroup(specificationId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecification(request, specificationId).flatMap { _ =>
      specificationManager.removeSpecificationFromGroup(specificationId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def addSpecificationToGroup(groupId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId).flatMap { _ =>
      val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPEC).toLong
      authorizationManager.canManageSpecification(request, specificationId).flatMap { _ =>
        specificationManager.addSpecificationToGroup(specificationId, groupId).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def copySpecificationToGroup(groupId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId).flatMap { _ =>
      val specificationId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPEC).toLong
      authorizationManager.canManageSpecification(request, specificationId).flatMap { _ =>
        specificationManager.copySpecificationToGroup(specificationId, groupId).map { newSpecificationId =>
          ResponseConstructor.constructJsonResponse(JsonUtil.jsId(newSpecificationId).toString())
        }
      }
    }
  }

  def getSpecificationOfActor(actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewActor(request, actorId).flatMap { _ =>
      specificationManager.getSpecificationOfActor(actorId).map { specification =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecification(specification).toString())
      }
    }
  }

  def getSpecificationIdOfActor(actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewActor(request, actorId).flatMap { _ =>
      specificationManager.getSpecificationIdOfActor(actorId).map { specificationId =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsId(specificationId).toString())
      }
    }
  }

  def getSpecificationGroups(): Action[AnyContent] = authorizedAction.async { request =>
    val domainId = ParameterExtractor.requiredQueryParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canViewDomains(request, Some(List(domainId))).flatMap { _ =>
      specificationManager.getSpecificationGroups(domainId).map { groups =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroups(groups).toString())
      }
    }
  }

  def getDomainSpecificationGroups(domainId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      specificationManager.getSpecificationGroupsByDomainIds(Some(List(domainId))).map { groups =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroups(groups).toString())
      }
    }
  }

  def getSpecificationGroupsOfDomains(): Action[AnyContent] = authorizedAction.async { request =>
    val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
    authorizationManager.canViewDomains(request, domainIds).flatMap { _ =>
      specificationManager.getSpecificationGroupsByDomainIds(domainIds).map { groups =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroups(groups).toString())
      }
    }
  }

  def getSpecificationGroup(groupId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecificationGroup(request, groupId).flatMap { _ =>
      specificationManager.getSpecificationGroupById(groupId).map { group =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroup(group, withApiKeys = true).toString())
      }
    }
  }

  def saveSpecificationOrder(): Action[AnyContent] = authorizedAction.async { request =>
    val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS).getOrElse(List.empty)
      val groupOrders = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_ORDERS).getOrElse(List.empty)
      val specIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS).getOrElse(List.empty)
      val specOrders = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_ORDERS).getOrElse(List.empty)
      specificationManager.saveSpecificationOrder(groupIds, groupOrders, specIds, specOrders).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def resetSpecificationOrder(): Action[AnyContent] = authorizedAction.async { request =>
    val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
    authorizationManager.canManageDomain(request, domainId).flatMap { _ =>
      specificationManager.resetSpecificationOrder(domainId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def getBadgeForStatus(specId: Long, status: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecification(request, specId).map { _ =>
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

}
