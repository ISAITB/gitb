package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import javax.inject.Inject
import managers.{AuthorizationManager, LandingPageManager}
import models.Constants
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.{HtmlUtil, JsonUtil}

/**
 * Created by VWYNGAET on 25/11/2016.
 */
class LandingPageService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, landingPageManager: LandingPageManager, authorizationManager: AuthorizationManager) extends AbstractController(cc) {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[LandingPageService])

  /**
   * Gets all landing pages for the specified community
   */
  def getLandingPagesByCommunity(communityId: Long) = authorizedAction { request =>
    authorizationManager.canManageLandingPages(request, communityId)
    val list = landingPageManager.getLandingPagesByCommunityWithoutContent(communityId)
    val json: String = JsonUtil.jsLandingPages(list).toString
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Creates new landing page
   */
  def createLandingPage() = authorizedAction { request =>
    val landingPage = ParameterExtractor.extractLandingPageInfo(request)
    authorizationManager.canManageLandingPages(request, landingPage.community)
    val name = landingPageManager.checkUniqueName(landingPage.name, landingPage.community)
    if (name) {
      landingPageManager.createLandingPage(landingPage)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A landing page with this name already exists.", Some("name"))
    }
  }

  /**
    * Gets the landing page with specified id
    */
  def getLandingPageById(pageId: Long) = authorizedAction { request =>
    authorizationManager.canManageLandingPage(request, pageId)
    val landingPage = landingPageManager.getLandingPageById(pageId)
    val json: String = JsonUtil.serializeLandingPage(landingPage)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
    * Updates landing page
    */
  def updateLandingPage(pageId: Long) = authorizedAction { request =>
    authorizationManager.canManageLandingPage(request, pageId)
    val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
    val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
    val content = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT))
    val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
    val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong

    val uniqueName = landingPageManager.checkUniqueName(pageId, name, communityId)
    if (uniqueName) {
      landingPageManager.updateLandingPage(pageId, name, description, content, default, communityId)
      ResponseConstructor.constructEmptyResponse
    } else {
      ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A landing page with this name already exists.", Some("name"))
    }
  }

  /**
   * Deletes landing page with specified id
   */
  def deleteLandingPage(pageId: Long) = authorizedAction { request =>
    authorizationManager.canManageLandingPage(request, pageId)
    landingPageManager.deleteLandingPage(pageId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Gets the default landing page for given community
   */
  def getCommunityDefaultLandingPage() = authorizedAction { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewDefaultLandingPage(request, communityId)
    val landingPage = landingPageManager.getCommunityDefaultLandingPage(communityId)
    val json: String = JsonUtil.serializeLandingPage(landingPage)
    ResponseConstructor.constructJsonResponse(json)
  }


}