package controllers

import controllers.util.ResponseConstructor
import managers.UserManager
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil

/**
 * Created by VWYNGAET on 25/10/2016.
 */
class UserService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[UserService])

  /**
   * Gets system administrator users
   */
  def getSystemAdministrators() = Action.async {
    UserManager.getSystemAdministrators() map { list =>
      val json: String = JsonUtil.jsUsers(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Gets users by organization
   */
  def getUsersByOrganization(orgId: Long) = Action.async {
    UserManager.getUsersByOrganization(orgId) map { list =>
      val json: String = JsonUtil.jsUsers(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

}
