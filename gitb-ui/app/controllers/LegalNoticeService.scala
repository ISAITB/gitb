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
import exceptions.ErrorCodes
import managers.{AuthorizationManager, LegalNoticeManager}
import models.Constants
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{HtmlUtil, JsonUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LegalNoticeService @Inject() (authorizedAction: AuthorizedAction,
                                    cc: ControllerComponents,
                                    legalNoticeManager: LegalNoticeManager,
                                    authorizationManager: AuthorizationManager)
                                   (implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Gets all legal notices for the specified community
   */
  def getLegalNoticesByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageLegalNotices(request, communityId).flatMap { _ =>
      legalNoticeManager.getLegalNoticesByCommunityWithoutContent(communityId).map { list =>
        val json: String = JsonUtil.jsLegalNotices(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Creates new legal notice
   */
  def createLegalNotice(): Action[AnyContent] = authorizedAction.async { request =>
    val legalNotice = ParameterExtractor.extractLegalNoticeInfo(request)
    authorizationManager.canManageLegalNotices(request, legalNotice.community).flatMap { _ =>
      legalNoticeManager.checkUniqueName(legalNotice.name, legalNotice.community).flatMap { uniqueName =>
        if (uniqueName) {
          legalNoticeManager.createLegalNotice(legalNotice).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A legal notice with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  /**
   * Gets the legal notice with specified id
   */
  def getLegalNoticeById(noticeId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageLegalNotice(request, noticeId).flatMap { _ =>
      legalNoticeManager.getLegalNoticeById(noticeId).map { notice =>
        val json: String = JsonUtil.serializeLegalNotice(notice)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Updates legal notice
   */
  def updateLegalNotice(noticeId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageLegalNotice(request, noticeId).flatMap { _ =>
      val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
      val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
      val content = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT))
      val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
      val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
      legalNoticeManager.checkUniqueName(noticeId, name, communityId).flatMap { uniqueName =>
        if (uniqueName) {
          legalNoticeManager.updateLegalNotice(noticeId, name, description, content, default, communityId).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A legal notice with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  /**
   * Deletes legal notice with specified id
   */
  def deleteLegalNotice(noticeId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageLegalNotice(request, noticeId).flatMap { _ =>
      legalNoticeManager.deleteLegalNotice(noticeId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
    * Gets the default landing page for given community
    */
  def getCommunityDefaultLegalNotice(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    for {
      _ <- authorizationManager.canViewDefaultLegalNotice(request, communityId)
      communityNotice <- legalNoticeManager.getCommunityDefaultLegalNotice(communityId)
      noticeToUse <- {
        if (communityNotice.isDefined) {
          Future.successful(communityNotice)
        } else {
          legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId)
        }
      }
      result <- {
        val json: String = JsonUtil.serializeLegalNotice(noticeToUse)
        Future.successful(ResponseConstructor.constructJsonResponse(json))
      }
    } yield result
  }

  /**
    * Gets the default landing page for the test bed.
    */
  def getTestBedDefaultLegalNotice(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestBedDefaultLegalNotice(request).flatMap { _ =>
      legalNoticeManager.getCommunityDefaultLegalNotice(Constants.DefaultCommunityId).map { legalNotice =>
        val json: String = JsonUtil.serializeLegalNotice(legalNotice)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

}