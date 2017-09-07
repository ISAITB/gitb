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
   * Gets all landing pages for the specified community
   */
  def getLandingPagesByCommunity(communityId: Long) = Action.async {
    LandingPageManager.getLandingPagesByCommunity(communityId) map { list =>
      val json: String = JsonUtil.jsLandingPages(list).toString
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  /**
   * Creates new landing page
   */
  def createLandingPage() = Action.async { request =>
    val landingPage = ParameterExtractor.extractLandingPageInfo(request)
    LandingPageManager.checkUniqueName(landingPage.name, landingPage.community) map { uniqueName =>
      if (uniqueName) {
        LandingPageManager.createLandingPage(landingPage)
        ResponseConstructor.constructEmptyResponse
      } else {
        ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Landing page with '" + landingPage.name + "' already exists.")
      }
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
    * Updates landing page
    */
  def updateLandingPage(pageId: Long) = Action.async { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT)
    val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong

    LandingPageManager.checkUniqueName(pageId, name, communityId) map { uniqueName =>
      if (uniqueName) {
        LandingPageManager.updateLandingPage(pageId, name, description, content, default, communityId)
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

  /**
   * Gets the default legal notice for given community
   */
  def getCommunityDefaultLandingPage() = Action.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    LandingPageManager.getCommunityDefaultLandingPage(communityId) map { landingPage =>
      val json: String = JsonUtil.serializeLandingPage(landingPage)
      ResponseConstructor.constructJsonResponse(json)
    }
  }


}