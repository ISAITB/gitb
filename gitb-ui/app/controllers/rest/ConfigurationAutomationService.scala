package controllers.rest

import controllers.util.{AuthorizedAction, RequestWithAttributes, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager, DomainParameterManager, ParameterManager}
import models.Constants
import models.automation.DomainParameterInfo
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class ConfigurationAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                                cc: ControllerComponents,
                                                authorizationManager: AuthorizationManager,
                                                domainParameterManager: DomainParameterManager,
                                                parameterManager: ParameterManager,
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

  def deleteDomainParameter(parameter: String): Action[AnyContent] = authorizedAction { request =>
    deleteDomainParameterInternal(None, parameter, request)
  }

  def deleteDomainParameterForDomain(domain: String, parameter: String): Action[AnyContent] = authorizedAction { request =>
    deleteDomainParameterInternal(Some(domain), parameter, request)
  }

  private def deleteDomainParameterInternal(domain: Option[String], parameter: String, request: RequestWithAttributes[AnyContent]): Result = {
    process(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      domainParameterManager.deleteDomainParameterThroughAutomationApi(communityKey, DomainParameterInfo(parameter, domain))
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

  def deleteOrganisationProperty(property: String): Action[AnyContent] = authorizedAction { request =>
    process(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      communityManager.deleteOrganisationParameterDefinitionThroughAutomationApi(communityKey, property)
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

  def deleteSystemProperty(propertyKey: String): Action[AnyContent] = authorizedAction { request =>
    process(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      communityManager.deleteSystemParameterDefinitionThroughAutomationApi(communityKey, propertyKey)
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

  def createStatementProperty(actor: String): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      parameterManager.createParameterDefinitionThroughAutomationApi(communityKey, actor, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def deleteStatementProperty(actor: String, property: String): Action[AnyContent] = authorizedAction { request =>
    process(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      parameterManager.deleteParameterDefinitionThroughAutomationApi(communityKey, actor, property)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def updateStatementProperty(actor: String): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageConfigurationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      parameterManager.updateParameterDefinitionThroughAutomationApi(communityKey, actor, input)
      ResponseConstructor.constructEmptyResponse
    })
  }

}
