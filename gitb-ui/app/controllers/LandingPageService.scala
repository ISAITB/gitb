package controllers

import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import exceptions.ErrorCodes
import managers.LandingPageManager
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import utils.JsonUtil
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

/**
 * Created by VWYNGAET on 25/11/2016.
 */
class LandingPageService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[LandingPageService])

  /**
   * Gets all landing pages
   */
  def getLandingPages() = Action.async {
    LandingPageManager.getLandingPages() map { list =>
      val json: String = JsonUtil.jsLandingPages(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Gets the landing page with specified id
   */
  def getLandingPageById(pageId: Long) = Action.async { request =>
    LandingPageManager.getLandingPageById(pageId) map { landingPage =>
      val json: String = JsonUtil.serializeLandingPage(landingPage)
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Gets the default landing page
   */
  def getDefaultLandingPage() = Action.async { request =>
    LandingPageManager.getDefaultLandingPage() map { landingPage =>
      if (landingPage != null) {
        val json: String = JsonUtil.serializeLandingPage(landingPage)
        ResponseConstructor.constructJsonResponse(json)
      } else {
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Creates new landing page
   */
  def createLandingPage() = Action.async { request =>
    val landingPage = ParameterExtractor.extractLandingPageInfo(request)
    LandingPageManager.checkUniqueName(landingPage.name) map { uniqueName =>
      if (uniqueName) {
        LandingPageManager.createLandingPage(landingPage)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Landing page with '" + landingPage.name + "' already exists.")
      }
    }
  }

  /**
   * Updates landing page
   */
  def updateLandingPage(pageId: Long) = Action.async { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT)
    val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean

    LandingPageManager.checkUniqueName(name, pageId) map { uniqueName =>
      if (uniqueName) {
        LandingPageManager.updateLandingPage(pageId, name, description, content, default)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Landing page with '" + name + "' already exists.")
      }
    }
  }

    /**
     * Deletes landing page with specified id
     */
    def deleteLandingPage(pageId: Long) = Action.async { request =>
      LandingPageManager.deleteLandingPage(pageId) map { unit =>
        ResponseConstructor.constructEmptyResponse
      }
    }

  }
