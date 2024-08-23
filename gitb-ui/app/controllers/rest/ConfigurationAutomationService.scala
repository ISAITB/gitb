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

  def createDomainParameter(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsDomainParameterConfiguration(body)
      domainParameterManager.createDomainParameterThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def deleteDomainParameter(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsDomainParameterConfiguration(body)
      domainParameterManager.deleteDomainParameterThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def updateDomainParameter(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsDomainParameterConfiguration(body)
      domainParameterManager.updateDomainParameterThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def createOrganisationProperty(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.createOrganisationParameterDefinitionThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def deleteOrganisationProperty(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.deleteOrganisationParameterDefinitionThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def updateOrganisationProperty(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.updateOrganisationParameterDefinitionThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def createSystemProperty(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.createSystemParameterDefinitionThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def deleteSystemProperty(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.deleteSystemParameterDefinitionThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def updateSystemProperty(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.updateSystemParameterDefinitionThroughAutomationApi(communityKey, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

}
