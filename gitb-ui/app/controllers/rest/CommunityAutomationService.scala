package controllers.rest

import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager}
import models.Constants
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class CommunityAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                            cc: ControllerComponents,
                                            communityManager: CommunityManager,
                                            authorizationManager: AuthorizationManager) extends BaseAutomationService(cc) {

  def createCommunity(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canCreateCommunityThroughAutomationApi), { body =>
      val input = JsonUtil.parseJsCreateCommunityRequest(body)
      val savedApiKey = communityManager.createCommunityThroughAutomationApi(input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
    })
  }

  def deleteCommunity(community: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canDeleteCommunityThroughAutomationApi(request)
    try {
      communityManager.deleteCommunityThroughAutomationApi(community)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def updateCommunity(community: String): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageAnyCommunityThroughAutomationApi), { body =>
      val input = JsonUtil.parseJsUpdateCommunityRequest(body, community)
      communityManager.updateCommunityThroughAutomationApi(input, allowDomainChange = true)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def updateOwnCommunity(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageCommunityThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateCommunityRequest(body, communityKey)
      communityManager.updateCommunityThroughAutomationApi(input, allowDomainChange = false)
      ResponseConstructor.constructEmptyResponse
    })
  }

}
