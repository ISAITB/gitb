package controllers

import java.io.InputStream
import java.nio.charset.StandardCharsets
import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes

import javax.inject.Inject
import managers.{AuthorizationManager, SystemConfigurationManager}
import models.Constants
import org.apache.commons.io.IOUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.JsString
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.{HtmlUtil, JsonUtil}

import scala.concurrent.ExecutionContext

class SystemConfigurationService @Inject()(implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, systemConfigurationManager: SystemConfigurationManager, environment: play.api.Environment, authorizationManager: AuthorizationManager) extends AbstractController(cc) {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SystemConfigurationService])

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
  }

  /**
   * Update system configuration
   */
  def updateSessionAliveTime = authorizedAction { request =>
    authorizationManager.canEditTheSessionAliveTime(request)

    val value = ParameterExtractor.optionalBodyParameter(request, Parameters.PARAMETER)

    if (value.isDefined && !isPositiveInt(value.get)) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_PARAM, "Value should be a positive integer.")
    } else {
      systemConfigurationManager.updateSystemParameter(Constants.SessionAliveTime, value)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getCssForTheme = authorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    val env = sys.env.get(Constants.EnvironmentTheme)
    ResponseConstructor.constructCssResponse(parseTheme(env))
  }

  def getFaviconForTheme = authorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    Ok.sendResource(systemConfigurationManager.getFaviconPath(), environment.classLoader, true)
  }

  def getLogo = authorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    ResponseConstructor.constructStringResponse(systemConfigurationManager.getLogoPath())
  }

  def getFooterLogo = authorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    ResponseConstructor.constructStringResponse(systemConfigurationManager.getFooterLogoPath())
  }

  private def isPositiveInt(value: String): Boolean = {
    value.matches("^[1-9]\\d*$")
  }

  private def parseTheme(theme: Option[String]): String = {
    if (theme.isDefined && theme.get == Constants.EcTheme) {
      IOUtils.toString(getInputStream("public/stylesheets/css/theme-ec.css"), StandardCharsets.UTF_8)
    } else {
      IOUtils.toString(getInputStream("public/stylesheets/css/theme-gitb.css"), StandardCharsets.UTF_8)
    }
  }

  private def getInputStream(path: String): InputStream = {
    environment.classLoader.getResourceAsStream(path)
  }

}
