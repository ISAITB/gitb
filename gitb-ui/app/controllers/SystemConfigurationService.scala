package controllers

import java.io.InputStream

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.SystemConfigurationManager
import models.Constants
import org.apache.commons.io.IOUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{Action, Controller}
import utils.JsonUtil

class SystemConfigurationService @Inject()(systemConfigurationManager: SystemConfigurationManager, environment: play.api.Environment) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[SystemConfigurationService])

  /**
   * Gets session alive time
   */
  def getSessionAliveTime = {
    getSystemConfiguration(Constants.SessionAliveTime)
  }

  /**
   * Update system configuration
   */
  def updateSessionAliveTime = Action.apply { request =>
    val value = ParameterExtractor.optionalBodyParameter(request, Parameters.PARAMETER)

    if (value.isDefined && !isPositiveInt(value.get)) {
      ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_PARAM, "Value should be a positive integer.")
    } else {
      systemConfigurationManager.updateSystemParameter(Constants.SessionAliveTime, value)
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getCssForTheme = Action { request =>
    val env = sys.env.get(Constants.EnvironmentTheme)
    ResponseConstructor.constructCssResponse(parseTheme(env))
  }

  def getLogo = Action { request =>
    val env = sys.env.get(Constants.EnvironmentTheme)
    ResponseConstructor.constructStringResponse(parseLogo(env))
  }

  def getFooterLogo = Action { request =>
    val env = sys.env.get(Constants.EnvironmentTheme)
    ResponseConstructor.constructStringResponse(parseFooterLogo(env))
  }

  /**
   * Gets the system configuration with specified name
   */
  private def getSystemConfiguration(name: String) = Action.apply { request =>
    val config = systemConfigurationManager.getSystemConfiguration(name)
    val json: String = JsonUtil.serializeSystemConfig(config)
    ResponseConstructor.constructJsonResponse(json)
  }

  private def isPositiveInt(value: String): Boolean = {
    value.matches("^[1-9]\\d*$")
  }

  private def parseTheme(theme: Option[String]): String = {
    if (theme.isDefined && theme.get == Constants.EcTheme) {
      IOUtils.toString(getInputStream("public/stylesheets/css/theme-ec.css"))
    } else {
      IOUtils.toString(getInputStream("public/stylesheets/css/theme-gitb.css"))
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
