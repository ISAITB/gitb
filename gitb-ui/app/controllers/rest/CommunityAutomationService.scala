package controllers.rest

import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager, OrganizationManager, SystemManager}
import models.Constants
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class CommunityAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                            cc: ControllerComponents,
                                            communityManager: CommunityManager,
                                            organisationManager: OrganizationManager,
                                            systemManager: SystemManager,
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

  def createOrganisation(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageOrganisationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateOrganisationRequest(body, communityKey)
      val savedApiKey = organisationManager.createOrganisationThroughAutomationApi(input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
    })
  }

  def deleteOrganisation(organisation: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageOrganisationThroughAutomationApi(request)
    try {
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      organisationManager.deleteOrganisationThroughAutomationApi(organisation, communityKey)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def updateOrganisation(organisation: String): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageOrganisationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateOrganisationRequest(body, organisation, communityKey)
      organisationManager.updateOrganisationThroughAutomationApi(input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def createSystem(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSystemThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateSystemRequest(body, communityKey)
      val savedApiKey = systemManager.createSystemThroughAutomationApi(input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
    })
  }

  def deleteSystem(system: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageSystemThroughAutomationApi(request)
    try {
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      systemManager.deleteSystemThroughAutomationApi(system, communityKey)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def updateSystem(system: String): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSystemThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateSystemRequest(body, system, communityKey)
      systemManager.updateSystemThroughAutomationApi(input)
      ResponseConstructor.constructEmptyResponse
    })
  }

}
