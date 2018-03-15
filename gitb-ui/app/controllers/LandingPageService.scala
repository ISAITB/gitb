package controllers

import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import managers.LandingPageManager
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{Action, Controller}
import utils.JsonUtil

/**
 * Created by VWYNGAET on 25/11/2016.
 */
class LandingPageService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[LandingPageService])

  /**
   * Gets all landing pages for the specified community
   */
  def getLandingPagesByCommunity(communityId: Long) = Action.apply {
    val list = LandingPageManager.getLandingPagesByCommunity(communityId)
    val json: String = JsonUtil.jsLandingPages(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new landing page
   */
  def createLandingPage() = Action.apply { request =>
    val landingPage = ParameterExtractor.extractLandingPageInfo(request)
    val name = LandingPageManager.checkUniqueName(landingPage.name, landingPage.community)
    if (name) {
      LandingPageManager.createLandingPage(landingPage)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Landing page with '" + landingPage.name + "' already exists.")
    }
  }

  /**
    * Gets the landing page with specified id
    */
  def getLandingPageById(pageId: Long) = Action.apply { request =>
    val landingPage = LandingPageManager.getLandingPageById(pageId)
    val json: String = JsonUtil.serializeLandingPage(landingPage)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Updates landing page
    */
  def updateLandingPage(pageId: Long) = Action.apply { request =>
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT)
    val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong

    val uniqueName = LandingPageManager.checkUniqueName(pageId, name, communityId)
    if (uniqueName) {
      LandingPageManager.updateLandingPage(pageId, name, description, content, default, communityId)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "Landing page with '" + name + "' already exists.")
    }
  }

  /**
   * Deletes landing page with specified id
   */
  def deleteLandingPage(pageId: Long) = Action.apply { request =>
    LandingPageManager.deleteLandingPage(pageId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Gets the default legal notice for given community
   */
  def getCommunityDefaultLandingPage() = Action.apply { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    val landingPage = LandingPageManager.getCommunityDefaultLandingPage(communityId)
    val json: String = JsonUtil.serializeLandingPage(landingPage)
    ResponseConstructor.constructJsonResponse(json)
  }


}