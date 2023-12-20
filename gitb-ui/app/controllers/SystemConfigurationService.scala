package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthorizationManager, SystemConfigurationManager}
import models.Constants
import org.apache.commons.io.FileUtils
import play.api.libs.json.JsString
import play.api.mvc.{AbstractController, ControllerComponents, Result}
import utils.{HtmlUtil, JsonUtil, RepositoryUtils}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SystemConfigurationService @Inject()(implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, repositoryUtils: RepositoryUtils, systemConfigurationManager: SystemConfigurationManager, environment: play.api.Environment, authorizationManager: AuthorizationManager) extends AbstractController(cc) {

  def getConfigurationValues() = authorizedAction { request =>
    authorizationManager.canViewSystemConfigurationValues(request)
    val configs = systemConfigurationManager.getEditableSystemConfigurationValues()
    val json: String = JsonUtil.jsSystemConfigurations(configs).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  def updateConfigurationValue() = authorizedAction { request =>
    authorizationManager.canUpdateSystemConfigurationValues(request)
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    var value = ParameterExtractor.optionalBodyParameter(request, Parameters.PARAMETER)
    if (systemConfigurationManager.isEditableSystemParameter(name)) {
      if (name == Constants.WelcomeMessage && value.isDefined) {
        value = Some(HtmlUtil.sanitizeEditorContent(value.get))
        if (value.get.isBlank) {
          value = None
        }
      }
      val resultToReport = systemConfigurationManager.updateSystemParameter(name, value)
      if (resultToReport.isDefined) {
        ResponseConstructor.constructJsonResponse(JsString(resultToReport.get).toString)
      } else {
        ResponseConstructor.constructEmptyResponse
      }
    } else {
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getCssForTheme = authorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    ResponseConstructor.constructCssResponse(systemConfigurationManager.getCssForActiveTheme())
  }

  def getThemeResource(themeId: Long, resourceName: String) = authorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    val resourceFile = systemConfigurationManager.getThemeResource(themeId, resourceName)
    if (resourceFile.isDefined) {
      Ok.sendFile(content = resourceFile.get)
    } else {
      NotFound
    }
  }

  private def streamThemeResource(resourcePath: String, themeId: Option[Long]): Result = {
    if (systemConfigurationManager.isBuiltInThemeResource(resourcePath)) {
      // Built-in resource shared under /assets/. Transform to /public/ for the classpath lookup.
      val resourceClassloaderPath = systemConfigurationManager.adaptBuiltInThemeResourcePathForClasspathLookup(resourcePath)
      Ok.sendResource(resourceClassloaderPath, environment.classLoader, inline = true)
    } else {
      // Custom resource.
      if (themeId.isDefined) {
        val themeResource = repositoryUtils.getThemeResource(themeId.get, resourcePath)
        if (themeResource.isDefined) {
          Ok.sendFile(content = themeResource.get)
        } else {
          NotFound
        }
      } else {
        NotFound
      }
    }
  }

  def previewThemeResource = authorizedAction { request =>
    authorizationManager.canManageThemes(request)
    val themeId = ParameterExtractor.optionalLongQueryParameter(request, Parameters.ID)
    val resourcePath = ParameterExtractor.requiredQueryParameter(request, Parameters.NAME)
    streamThemeResource(resourcePath, themeId)
  }

  def getFaviconForTheme = authorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    val activeThemeId = systemConfigurationManager.getActiveThemeId()
    val faviconPath = systemConfigurationManager.getFaviconPath()
    streamThemeResource(faviconPath, Some(activeThemeId))
  }

  def getThemes = authorizedAction { request =>
    authorizationManager.canManageThemes(request)
    val themes = systemConfigurationManager.getThemes()
    ResponseConstructor.constructJsonResponse(JsonUtil.jsThemes(themes).toString)
  }

  def getTheme(themeId: Long) = authorizedAction { request =>
    authorizationManager.canManageThemes(request)
    val theme = systemConfigurationManager.getTheme(themeId)
    ResponseConstructor.constructJsonResponse(JsonUtil.jsTheme(theme).toString)
  }

  def createTheme = authorizedAction { request =>
    try {
      authorizationManager.canManageThemes(request)
      val paramMap = ParameterExtractor.paramMap(request)
      val themeData = ParameterExtractor.extractTheme(request, paramMap)
      if (themeData._3.isDefined) {
        themeData._3.get
      } else if (themeData._1.isDefined && themeData._2.isDefined) {
        if (systemConfigurationManager.themeExists(themeData._1.get.key, None)) {
          ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A theme with this key already exists.")
        } else {
          val referenceThemeId = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.REFERENCE).toLong
          systemConfigurationManager.createTheme(referenceThemeId, themeData._1.get, themeData._2.get)
          ResponseConstructor.constructEmptyResponse
        }
      } else {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Missing expected information.")
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def activateTheme(themeId: Long) = authorizedAction { request =>
    authorizationManager.canManageThemes(request)
    systemConfigurationManager.activateTheme(themeId)
    ResponseConstructor.constructEmptyResponse
  }

  def updateTheme(themeId: Long) = authorizedAction { request =>
    try {
      authorizationManager.canManageThemes(request)
      val paramMap = ParameterExtractor.paramMap(request)
      val themeData = ParameterExtractor.extractTheme(request, paramMap, Some(themeId))
      if (themeData._3.isDefined) {
        themeData._3.get
      } else if (themeData._1.isDefined && themeData._2.isDefined) {
        if (systemConfigurationManager.themeExists(themeData._1.get.key, Some(themeId))) {
          ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A theme with this key already exists.")
        } else {
          systemConfigurationManager.updateTheme(themeData._1.get, themeData._2.get)
          ResponseConstructor.constructEmptyResponse
        }
      } else {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Missing expected information.")
      }
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def deleteTheme(themeId: Long) = authorizedAction { request =>
    authorizationManager.canManageThemes(request)
    val deleted = systemConfigurationManager.deleteTheme(themeId)
    if (deleted) {
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_PARAM, "The selected theme cannot be deleted.")
    }
  }

}
