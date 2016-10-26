package controllers

import controllers.util.ResponseConstructor
import managers.OrganizationManager
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil

/**
 * Created by VWYNGAET on 26/10/2016.
 */
class OrganizationService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[OrganizationService])

  /**
   * Gets all organizations
   */
  def getOrganizations() = Action.async {
    OrganizationManager.getOrganizations() map { list =>
      val json: String = JsonUtil.jsOrganizations(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }
}
