package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, ErrorTemplateManager}
import models.Constants
import play.api.mvc.Controller
import utils.{HtmlUtil, JsonUtil}

class ErrorTemplateService @Inject() (errorTemplateManager: ErrorTemplateManager, authorizationManager: AuthorizationManager) extends Controller {

  /**
   * Gets all error templates for the specified community
   */
  def getErrorTemplatesByCommunity(communityId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageErrorTemplates(request, communityId)
    val list = errorTemplateManager.getErrorTemplatesByCommunity(communityId)
    val json: String = JsonUtil.jsErrorTemplates(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new error template
   */
  def createErrorTemplate() = AuthorizedAction { request =>
    val errorTemplate = ParameterExtractor.extractErrorTemplateInfo(request)
    authorizationManager.canManageErrorTemplates(request, errorTemplate.community)
    val uniqueName = errorTemplateManager.checkUniqueName(errorTemplate.name, errorTemplate.community)
    if (uniqueName) {
      errorTemplateManager.createErrorTemplate(errorTemplate)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Error template with name '" + errorTemplate.name + "' already exists.")
    }
  }

  /**
   * Gets the error template with specified id
   */
  def getErrorTemplateById(templateId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageErrorTemplate(request, templateId)
    val ln = errorTemplateManager.getErrorTemplateById(templateId)
    val json: String = JsonUtil.serializeErrorTemplate(Some(ln))
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates error template
   */
  def updateErrorTemplate(templateId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageErrorTemplate(request, templateId)
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT))
    val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong

    val uniqueName = errorTemplateManager.checkUniqueName(templateId, name, communityId)
    if (uniqueName) {
      errorTemplateManager.updateErrorTemplate(templateId, name, description, content, default, communityId)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Error template with name '" + name + "' already exists.")
    }
  }

  /**
   * Deletes error template with specified id
   */
  def deleteErrorTemplate(templateId: Long) = AuthorizedAction { request =>
    authorizationManager.canManageErrorTemplate(request, templateId)
    errorTemplateManager.deleteErrorTemplate(templateId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Gets the default error template for given community
    */
  def getCommunityDefaultErrorTemplate() = AuthorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewDefaultErrorTemplate(request, communityId)
    var errorTemplate = errorTemplateManager.getCommunityDefaultErrorTemplate(communityId)
    if (errorTemplate.isEmpty) {
      errorTemplate = errorTemplateManager.getCommunityDefaultErrorTemplate(Constants.DefaultCommunityId)
    }
    val json: String = JsonUtil.serializeErrorTemplate(errorTemplate)
    ResponseConstructor.constructJsonResponse(json)
  }


}