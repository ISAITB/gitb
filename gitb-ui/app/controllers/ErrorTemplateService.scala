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
import exceptions.ErrorCodes

import javax.inject.Inject
import managers.{AuthorizationManager, ErrorTemplateManager}
import models.Constants
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{HtmlUtil, JsonUtil}

import scala.concurrent.{ExecutionContext, Future}

class ErrorTemplateService @Inject() (authorizedAction: AuthorizedAction,
                                      cc: ControllerComponents,
                                      errorTemplateManager: ErrorTemplateManager,
                                      authorizationManager: AuthorizationManager)
                                     (implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Gets all error templates for the specified community
   */
  def getErrorTemplatesByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageErrorTemplates(request, communityId).flatMap { _=>
      errorTemplateManager.getErrorTemplatesByCommunityWithoutContent(communityId).map { list =>
        val json: String = JsonUtil.jsErrorTemplates(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Creates new error template
   */
  def createErrorTemplate(): Action[AnyContent] = authorizedAction.async { request =>
    val errorTemplate = ParameterExtractor.extractErrorTemplateInfo(request)
    authorizationManager.canManageErrorTemplates(request, errorTemplate.community).flatMap { _ =>
      errorTemplateManager.checkUniqueName(errorTemplate.name, errorTemplate.community).flatMap { uniqueName =>
        if (uniqueName) {
          errorTemplateManager.createErrorTemplate(errorTemplate).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "An error template with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  /**
   * Gets the error template with specified id
   */
  def getErrorTemplateById(templateId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageErrorTemplate(request, templateId).flatMap { _ =>
      errorTemplateManager.getErrorTemplateById(templateId).map { template =>
        val json: String = JsonUtil.serializeErrorTemplate(template)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Updates error template
   */
  def updateErrorTemplate(templateId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageErrorTemplate(request, templateId).flatMap { _ =>
      val name = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
      val description = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESCRIPTION)
      val content = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, ParameterNames.CONTENT))
      val default = ParameterExtractor.requiredBodyParameter(request, ParameterNames.DEFAULT).toBoolean
      val communityId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.COMMUNITY_ID).toLong
      errorTemplateManager.checkUniqueName(templateId, name, communityId).flatMap { uniqueName =>
        if (uniqueName) {
          errorTemplateManager.updateErrorTemplate(templateId, name, description, content, default, communityId).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "An error template with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  /**
   * Deletes error template with specified id
   */
  def deleteErrorTemplate(templateId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageErrorTemplate(request, templateId).flatMap { _ =>
      errorTemplateManager.deleteErrorTemplate(templateId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
    * Gets the default error template for given community
    */
  def getCommunityDefaultErrorTemplate(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, ParameterNames.COMMUNITY_ID).toLong
    authorizationManager.canViewDefaultErrorTemplate(request, communityId).flatMap { _ =>
      errorTemplateManager.getCommunityDefaultErrorTemplate(communityId).flatMap { errorTemplate =>
        if (errorTemplate.isEmpty) {
          errorTemplateManager.getCommunityDefaultErrorTemplate(Constants.DefaultCommunityId)
        } else {
          Future.successful(errorTemplate)
        }
      }.map { errorTemplate =>
        val json: String = JsonUtil.serializeErrorTemplate(errorTemplate)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

}