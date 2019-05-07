package controllers

import java.io.InputStream
import java.nio.charset.Charset

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, SystemConfigurationManager}
import models.Constants
import org.apache.commons.io.IOUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{Action, Controller}
import utils.JsonUtil

class SystemConfigurationService @Inject()(systemConfigurationManager: SystemConfigurationManager, environment: play.api.Environment, authorizationManager: AuthorizationManager) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SystemConfigurationService])

  /**
   * Gets session alive time
   */
  def getSessionAliveTime = AuthorizedAction { request =>
    authorizationManager.canViewTheSessionAliveTime(request)
    val config = systemConfigurationManager.getSystemConfiguration(Constants.SessionAliveTime)
    val json: String = JsonUtil.serializeSystemConfig(config)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Update system configuration
   */
  def updateSessionAliveTime = AuthorizedAction { request =>
    authorizationManager.canEditTheSessionAliveTime(request)

    val value = ParameterExtractor.optionalBodyParameter(request, Parameters.PARAMETER)

    if (value.isDefined && !isPositiveInt(value.get)) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_PARAM, "Value should be a positive integer.")
    } else {
      systemConfigurationManager.updateSystemParameter(Constants.SessionAliveTime, value)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getCssForTheme = AuthorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    val env = sys.env.get(Constants.EnvironmentTheme)
    ResponseConstructor.constructCssResponse(parseTheme(env))
  }

  def getLogo = AuthorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    val env = sys.env.get(Constants.EnvironmentTheme)
    ResponseConstructor.constructStringResponse(parseLogo(env))
  }

  def getFooterLogo = AuthorizedAction { request =>
    authorizationManager.canAccessThemeData(request)
    val env = sys.env.get(Constants.EnvironmentTheme)
    ResponseConstructor.constructStringResponse(parseFooterLogo(env))
  }

  private def isPositiveInt(value: String): Boolean = {
    value.matches("^[1-9]\\d*$")
  }

  private def parseTheme(theme: Option[String]): String = {
    if (theme.isDefined && theme.get == Constants.EcTheme) {
      IOUtils.toString(getInputStream("public/stylesheets/css/theme-ec.css"), Charset.forName("UTF-8"))
    } else {
      IOUtils.toString(getInputStream("public/stylesheets/css/theme-gitb.css"), Charset.forName("UTF-8"))
    }
  }

  private def getInputStream(path: String): InputStream = {
    environment.classLoader.getResourceAsStream(path)
  }

  private def parseLogo(theme: Option[String]): String = {
    if (theme.isDefined && theme.get == Constants.EcTheme) {
      Constants.EcLogo
    } else {
      Constants.GitbLogo
    }
  }

  private def parseFooterLogo(theme: Option[String]): String = {
    if (theme.isDefined && theme.get == Constants.EcTheme) {
      Constants.GitbLogo
    } else {
      ""
    }
  }

}
