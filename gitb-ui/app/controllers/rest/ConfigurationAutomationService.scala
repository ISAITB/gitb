package controllers.rest

import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager, DomainParameterManager}
import models.Constants
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class ConfigurationAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                                cc: ControllerComponents,
                                                authorizationManager: AuthorizationManager,
                                                domainParameterManager: DomainParameterManager,
                                                communityManager: CommunityManager) extends BaseAutomationService(cc) {

  def configure(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsConfigurationRequest(body)
      val warnings = communityManager.applyConfigurationViaAutomationApi(communityKey, input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsMessages(warnings).toString())
    })
  }

  def createDomainParameters(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateDomainParametersRequest(body)
      val warnings = domainParameterManager.createDomainParametersThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsMessages(warnings).toString())
    })
  }

  def deleteDomainParameters(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsDeleteDomainParametersRequest(body)
      val warnings = domainParameterManager.deleteDomainParametersThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsMessages(warnings).toString())
    })
  }

  def updateDomainParameters(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateDomainParametersRequest(body)
      val warnings = domainParameterManager.updateDomainParametersThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsMessages(warnings).toString())
    })
  }

}
