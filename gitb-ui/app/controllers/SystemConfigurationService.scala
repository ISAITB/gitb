package controllers

import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import exceptions.ErrorCodes
import managers.SystemConfigurationManager
import org.apache.commons.io.IOUtils
import org.slf4j.{LoggerFactory, Logger}
import play.api.Play
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil
import models.Constants
import java.io.InputStream
import play.api.Play.current
import scala.concurrent.Future

class SystemConfigurationService extends Controller {
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
  def updateSessionAliveTime = Action.async { request =>
    val value = ParameterExtractor.optionalBodyParameter(request, Parameters.PARAMETER)

    if (value.isDefined && !isPositiveInt(value.get)) {
      Future {
        ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_PARAM, "Value should be a positive integer.")
      }
    } else {
      SystemConfigurationManager.updateSystemParameter(Constants.SessionAliveTime, value) map { unit =>
        ResponseConstructor.constructEmptyResponse
      }
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
  private def getSystemConfiguration(name: String) = Action.async { request =>
    SystemConfigurationManager.getSystemConfiguration(name) map { config =>
      val json: String = JsonUtil.serializeSystemConfig(config)
      ResponseConstructor.constructJsonResponse(json)
    }
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
    Play.classloader.getResourceAsStream(path)
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
