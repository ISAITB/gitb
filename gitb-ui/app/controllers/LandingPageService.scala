package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthorizationManager, LandingPageManager}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{HtmlUtil, JsonUtil}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by VWYNGAET on 25/11/2016.
 */
class LandingPageService @Inject() (authorizedAction: AuthorizedAction,
                                    cc: ControllerComponents,
                                    landingPageManager: LandingPageManager,
                                    authorizationManager: AuthorizationManager)
                                   (implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * Gets all landing pages for the specified community
   */
  def getLandingPagesByCommunity(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageLandingPages(request, communityId).flatMap { _ =>
      landingPageManager.getLandingPagesByCommunityWithoutContent(communityId).map { list =>
        val json: String = JsonUtil.jsLandingPages(list).toString
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Creates new landing page
   */
  def createLandingPage(): Action[AnyContent] = authorizedAction.async { request =>
    val landingPage = ParameterExtractor.extractLandingPageInfo(request)
    authorizationManager.canManageLandingPages(request, landingPage.community).flatMap { _ =>
      landingPageManager.checkUniqueName(landingPage.name, landingPage.community).flatMap { uniqueName =>
        if (uniqueName) {
          landingPageManager.createLandingPage(landingPage).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A landing page with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  /**
    * Gets the landing page with specified id
    */
  def getLandingPageById(pageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageLandingPage(request, pageId).flatMap { _ =>
      landingPageManager.getLandingPageById(pageId).map { landingPage =>
        val json: String = JsonUtil.serializeLandingPage(landingPage)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
    * Updates landing page
    */
  def updateLandingPage(pageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageLandingPage(request, pageId).flatMap { _ =>
      val name = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
      val description = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
      val content = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.CONTENT))
      val default = ParameterExtractor.requiredBodyParameter(request, Parameters.DEFAULT).toBoolean
      val communityId = ParameterExtractor.requiredBodyParameter(request, Parameters.COMMUNITY_ID).toLong
      landingPageManager.checkUniqueName(pageId, name, communityId).flatMap { uniqueName =>
        if (uniqueName) {
          landingPageManager.updateLandingPage(pageId, name, description, content, default, communityId).map { _ =>
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.NAME_EXISTS, "A landing page with this name already exists.", Some("name"))
          }
        }
      }
    }
  }

  /**
   * Deletes landing page with specified id
   */
  def deleteLandingPage(pageId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageLandingPage(request, pageId).flatMap { _ =>
      landingPageManager.deleteLandingPage(pageId).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  /**
   * Gets the default landing page for given community
   */
  def getCommunityDefaultLandingPage(): Action[AnyContent] = authorizedAction.async { request =>
    val communityId = ParameterExtractor.requiredQueryParameter(request, Parameters.COMMUNITY_ID).toLong
    authorizationManager.canViewDefaultLandingPage(request, communityId).flatMap { _ =>
      landingPageManager.getCommunityDefaultLandingPage(communityId).map { landingPage =>
        val json: String = JsonUtil.serializeLandingPage(landingPage)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

}