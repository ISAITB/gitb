package controllers.rest

import controllers.util.{AuthorizedAction, RequestWithAttributes, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager, DomainParameterManager, ParameterManager}
import models.Constants
import models.automation.DomainParameterInfo
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfigurationAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                                cc: ControllerComponents,
                                                authorizationManager: AuthorizationManager,
                                                domainParameterManager: DomainParameterManager,
                                                parameterManager: ParameterManager,
                                                communityManager: CommunityManager)
                                               (implicit ec: ExecutionContext) extends BaseAutomationService(cc) {

  def configure(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsConfigurationRequest(body)
      communityManager.applyConfigurationViaAutomationApi(communityKey, input).map { warnings =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsMessages(warnings).toString())
      }
    })
  }

  def createDomainParameter(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsDomainParameterConfiguration(body)
      domainParameterManager.createDomainParameterThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteDomainParameter(parameter: String): Action[AnyContent] = authorizedAction.async { request =>
    deleteDomainParameterInternal(None, parameter, request)
  }

  def deleteDomainParameterForDomain(domain: String, parameter: String): Action[AnyContent] = authorizedAction.async { request =>
    deleteDomainParameterInternal(Some(domain), parameter, request)
  }

  private def deleteDomainParameterInternal(domain: Option[String], parameter: String, request: RequestWithAttributes[AnyContent]): Future[Result] = {
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      domainParameterManager.deleteDomainParameterThroughAutomationApi(communityKey, DomainParameterInfo(parameter, domain)).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateDomainParameter(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsDomainParameterConfiguration(body)
      domainParameterManager.updateDomainParameterThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createOrganisationProperty(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.createOrganisationParameterDefinitionThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteOrganisationProperty(property: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      communityManager.deleteOrganisationParameterDefinitionThroughAutomationApi(communityKey, property).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateOrganisationProperty(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.updateOrganisationParameterDefinitionThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createSystemProperty(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.createSystemParameterDefinitionThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteSystemProperty(propertyKey: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      communityManager.deleteSystemParameterDefinitionThroughAutomationApi(communityKey, propertyKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateSystemProperty(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.updateSystemParameterDefinitionThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createStatementProperty(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      parameterManager.createParameterDefinitionThroughAutomationApi(communityKey, actor, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteStatementProperty(actor: String, property: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      parameterManager.deleteParameterDefinitionThroughAutomationApi(communityKey, actor, property).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateStatementProperty(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      parameterManager.updateParameterDefinitionThroughAutomationApi(communityKey, actor, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

}
