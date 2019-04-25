package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.ErrorTemplateManager
import play.api.mvc.{Action, Controller}
import utils.{HtmlUtil, JsonUtil}

class ErrorTemplateService @Inject() (errorTemplateManager: ErrorTemplateManager) extends Controller {

  /**
   * Gets all error templates for the specified community
   */
  def getErrorTemplatesByCommunity(communityId: Long) = Action.apply {
    val list = errorTemplateManager.getErrorTemplatesByCommunity(communityId)
    val json: String = JsonUtil.jsErrorTemplates(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new error template
   */
  def createErrorTemplate() = Action.apply { request =>
    val errorTemplate = ParameterExtractor.extractErrorTemplateInfo(request)
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
  def getErrorTemplateById(templateId: Long) = Action.apply { request =>
    val ln = errorTemplateManager.getErrorTemplateById(templateId)
    val json: String = JsonUtil.serializeErrorTemplate(ln)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Updates error template
   */
  def updateErrorTemplate(templateId: Long) = Action.apply { request =>
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
  def deleteErrorTemplate(templateId: Long) = Action.apply { request =>
    errorTemplateManager.deleteErrorTemplate(templateId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
    * Gets the default error template for given community
    */
  def getCommunityDefaultErrorTemplate() = Action.apply { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    val errorTemplate = errorTemplateManager.getCommunityDefaultErrorTemplate(communityId)
    val json: String = JsonUtil.serializeErrorTemplate(errorTemplate)
    ResponseConstructor.constructJsonResponse(json)
  }


}