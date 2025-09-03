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

import controllers.util.{AuthorizedAction, ParameterExtractor, ParameterNames, ResponseConstructor}
import managers.{ActorManager, AuthorizationManager, CommunityLabelManager}
import models.BadgeInfo
import models.Enums.{LabelType, TestResultStatus}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.RepositoryUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ActorService @Inject() (authorizedAction: AuthorizedAction,
                              cc: ControllerComponents,
                              repositoryUtils: RepositoryUtils,
                              actorManager: ActorManager,
                              authorizationManager: AuthorizationManager,
                              communityLabelManager: CommunityLabelManager)
                             (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def deleteActor(actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canDeleteActor(request, actorId).flatMap { _ =>
      actorManager.deleteActorWrapper(actorId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def updateActor(actorId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canUpdateActor(request, actorId).flatMap { _ =>
      val paramMap = ParameterExtractor.paramMap(request)
      val actor = ParameterExtractor.extractActor(paramMap)
      val specificationId = ParameterExtractor.requiredBodyParameter(paramMap, ParameterNames.SPECIFICATION_ID).toLong
      actorManager.checkActorExistsInSpecification(actor.actorId, specificationId, Some(actorId)).flatMap { actorExists =>
        if (actorExists) {
          communityLabelManager.getLabelsByUserId(ParameterExtractor.extractUserId(request)).map { labels =>
            ResponseConstructor.constructBadRequestResponse(500, communityLabelManager.getLabel(labels, LabelType.Actor) + " with this ID already exists in the " + communityLabelManager.getLabel(labels, LabelType.Specification, single = true, lowercase = true)+".")
          }
        } else {
          val badgeInfo = ParameterExtractor.extractBadges(request, paramMap, forReport = false)
          if (badgeInfo._2.nonEmpty) {
            Future.successful(badgeInfo._2.get)
          } else {
            val badgeInfoForReport = ParameterExtractor.extractBadges(request, paramMap, forReport = true)
            if (badgeInfoForReport._2.nonEmpty) {
              Future.successful(badgeInfoForReport._2.get)
            } else {
              actorManager.updateActorWrapper(actorId, actor.actorId, actor.name, actor.description, actor.reportMetadata, actor.default, actor.hidden, actor.displayOrder, specificationId, BadgeInfo(badgeInfo._1.get, badgeInfoForReport._1.get)).map { _ =>
                ResponseConstructor.constructEmptyResponse
              }
            }
          }
        }
      }
    }
  }

  def getBadgeForStatus(specId: Long, actorId: Long, status: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageSpecification(request, specId).map { _ =>
      val forReport = ParameterExtractor.optionalBooleanQueryParameter(request, ParameterNames.REPORT)
      val statusToLookup = TestResultStatus.withName(status).toString
      val badge = repositoryUtils.getConformanceBadge(specId, Some(actorId), None, statusToLookup, exactMatch = true, forReport.getOrElse(false))
      if (badge.isDefined && badge.get.exists()) {
        Ok.sendFile(content = badge.get)
      } else {
        NotFound
      }
    }
  }

}
