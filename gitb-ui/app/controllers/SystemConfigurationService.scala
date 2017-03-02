package controllers

import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import exceptions.ErrorCodes
import managers.SystemConfigurationManager
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil
import models.Constants

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

  def getTheme = Action { request =>
    val env = sys.env.get(Constants.EnvironmentTheme)
    val json: String = JsonUtil.jsEnvironmentVariable("theme", parseTheme(env.getOrElse(Constants.DefaultTheme))).toString()
    ResponseConstructor.constructJsonResponse(json)
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

  private def parseTheme(theme: String): String = {
    if (theme == Constants.EcTheme) theme else Constants.DefaultTheme
  }

}
