/*
 * Copyright (C) 2026 European Union
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
import exceptions.ErrorCodes
import managers.{AuthorizationManager, CustomContentManager, ServerEventManager}
import models.Enums.CustomContentType
import models.Constants
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import utils.{HtmlUtil, JsonUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomContentService @Inject() (authorizedAction: AuthorizedAction,
                                      cc: ControllerComponents,
                                      customContentManager: CustomContentManager,
                                      authorizationManager: AuthorizationManager,
                                      serverEventManager: ServerEventManager)
                                     (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def extractContentType(request: Request[AnyContent]): Short = {
    val contentType = ParameterExtractor.requiredQueryParameter(request, ParameterNames.TYPE).toShort
    if (!CustomContentType.All.contains(contentType)) {
      throw new IllegalArgumentException("Unsupported custom content type [" + contentType + "]")
    }
    contentType
  }

  /**
   * Runs a mutation, pushing a configuration update to clients if it changed the default legal notice of the Test Bed.
   */
  private def withConfigurationUpdate[T](contentType: Short, action: => Future[T]): Future[T] = {
    if (contentType == CustomContentType.LegalNotice) {
      customContentManager.detectingTestBedDefaultLegalNoticeChange(action).map { case (result, changed) =>
        if (changed) serverEventManager.publishConfiguration()
        result
      }
    } else {
      action
    }
  }

  /**
   * Gets the custom content of the specified community (without rich content), optionally restricted to the given types
   */
  def getCustomContentByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCustomContents(request, communityId).flatMap { _ =>
      val types = ParameterExtractor.optionalListQueryParameter(request, ParameterNames.TYPES)
        .map(_.map(_.toShort).toSet.intersect(CustomContentType.All))
        .getOrElse(CustomContentType.All)
      customContentManager.getCustomContentByCommunityWithoutContent(communityId, types).map { list =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsCustomContents(list).toString)
      }
    }
  }

  /**
   * Searches the custom content of the specified community, optionally restricted to the given types
   */
  def searchCustomContentByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCustomContents(request, communityId).flatMap { _ =>
      val page = ParameterExtractor.extractPageNumber(request)
      val limit = ParameterExtractor.extractPageLimit(request)
      val types = ParameterExtractor.optionalListQueryParameter(request, ParameterNames.TYPES)
        .map(_.map(_.toShort).toSet.intersect(CustomContentType.All))
        .getOrElse(CustomContentType.All)
      customContentManager.searchCustomContentByCommunityWithoutContent(communityId, types, page, limit).map { result =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSearchResult(result, JsonUtil.jsCustomContents).toString)
      }
    }
  }

  def createCustomContent(): Action[AnyContent] = authorizedAction.async { request =>
    val customContent = ParameterExtractor.extractCustomContentInfo(request)
    authorizationManager.canManageCustomContents(request, customContent.community).flatMap { _ =>
      customContentManager.checkUniqueName(customContent.name, customContent.community, customContent.contentType).flatMap { uniqueName =>
        if (uniqueName) {
          withConfigurationUpdate(customContent.contentType, customContentManager.createCustomContent(customContent)).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A " + CustomContentType.label(customContent.contentType) + " with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  def getCustomContentById(contentId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCustomContent(request, contentId).flatMap { _ =>
      customContentManager.getCustomContentById(contentId).map { customContent =>
        ResponseConstructor.constructJsonResponse(JsonUtil.serializeCustomContent(customContent))
      }
    }
  }

  def updateCustomContent(contentId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCustomContent(request, contentId).flatMap { _ =>
      val name = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
      val description = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESCRIPTION)
      val content = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, ParameterNames.CONTENT))
      val default = ParameterExtractor.requiredBodyParameter(request, ParameterNames.DEFAULT).toBoolean
      val communityId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
      customContentManager.getCustomContentById(contentId).flatMap { existing =>
        val contentType = existing.map(_.contentType).getOrElse(CustomContentType.LandingPage)
        customContentManager.checkUniqueName(contentId, name, communityId, contentType).flatMap { uniqueName =>
          if (uniqueName) {
            withConfigurationUpdate(contentType, customContentManager.updateCustomContent(contentId, name, description, content, default, communityId)).map { _ =>
              ResponseConstructor.constructEmptyResponse
            }
          } else {
            Future.successful {
              ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A " + CustomContentType.label(contentType) + " with this name already exists.", Some("name"))
            }
          }
        }
      }
    }
  }

  def deleteCustomContent(contentId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCustomContent(request, contentId).flatMap { _ =>
      customContentManager.getCustomContentById(contentId).flatMap { existing =>
        withConfigurationUpdate(existing.map(_.contentType).getOrElse(CustomContentType.LandingPage), customContentManager.deleteCustomContent(contentId)).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  /**
   * Gets the default content of the given type for the given community. Legal notices and error templates fall back
   * to the Test Bed default when the community has none.
   */
  def getCommunityDefaultCustomContent(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, ParameterNames.COMMUNITY_ID).toLong
    val contentType = extractContentType(request)
    authorizationManager.canViewDefaultCustomContent(request, communityId).flatMap { _ =>
      customContentManager.getCommunityDefaultCustomContent(communityId, contentType).flatMap { communityContent =>
        if (communityContent.isEmpty && contentType != CustomContentType.LandingPage) {
          customContentManager.getCommunityDefaultCustomContent(Constants.DefaultCommunityId, contentType)
        } else {
          Future.successful(communityContent)
        }
      }.map { customContent =>
        ResponseConstructor.constructJsonResponse(JsonUtil.serializeCustomContent(customContent))
      }
    }
  }

  /**
   * Gets the default legal notice of the Test Bed. This is the only content that is accessible without authentication.
   */
  def getTestBedDefaultLegalNotice(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestBedDefaultLegalNotice(request).flatMap { _ =>
      customContentManager.getCommunityDefaultCustomContent(Constants.DefaultCommunityId, CustomContentType.LegalNotice).map { customContent =>
        ResponseConstructor.constructJsonResponse(JsonUtil.serializeCustomContent(customContent))
      }
    }
  }

  /**
   * Gets the types of custom content for which the Test Bed has a default defined.
   */
  def getTestBedDefaultTypes(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewDefaultCustomContent(request, Constants.DefaultCommunityId).flatMap { _ =>
      customContentManager.getTestBedDefaultTypes().map { types =>
        ResponseConstructor.constructJsonResponse(JsonUtil.serializeTestBedDefaultTypes(types))
      }
    }
  }

}
