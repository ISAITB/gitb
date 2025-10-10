/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package controllers.rest

import controllers.util.{AuthorizedAction, ParameterExtractor, RequestWithAttributes, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager, DomainParameterManager, ParameterManager}
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
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsConfigurationRequest(body)
      communityManager.applyConfigurationViaAutomationApi(communityKey, input).map { warnings =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsMessages(warnings).toString())
      }
    })
  }

  def createDomainParameter(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
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
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      domainParameterManager.deleteDomainParameterThroughAutomationApi(communityKey, DomainParameterInfo(parameter, domain)).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateDomainParameter(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsDomainParameterConfiguration(body)
      domainParameterManager.updateDomainParameterThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createTestService(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsTestServiceConfiguration(body, isNew = true)
      domainParameterManager.createTestServiceThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateTestService(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsTestServiceConfiguration(body, isNew = false)
      domainParameterManager.updateTestServiceThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteTestService(parameter: String): Action[AnyContent] = authorizedAction.async { request =>
    deleteTestServiceInternal(None, parameter, request)
  }

  def deleteTestServiceForDomain(domain: String, parameter: String): Action[AnyContent] = authorizedAction.async { request =>
    deleteTestServiceInternal(Some(domain), parameter, request)
  }

  private def deleteTestServiceInternal(domain: Option[String], parameter: String, request: RequestWithAttributes[AnyContent]): Future[Result] = {
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      domainParameterManager.deleteTestServiceThroughAutomationApi(communityKey, domain, parameter).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def searchTestServices(): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val searchCriteria = ParameterExtractor.extractTestServiceSearchCriteria(request)
      domainParameterManager.searchTestServicesThroughAutomationApi(communityKey, searchCriteria).map { results =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestServicesForAutomationApi(results).toString())
      }
    })
  }

  def createOrganisationProperty(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.createOrganisationParameterDefinitionThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteOrganisationProperty(property: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      communityManager.deleteOrganisationParameterDefinitionThroughAutomationApi(communityKey, property).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateOrganisationProperty(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.updateOrganisationParameterDefinitionThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createSystemProperty(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.createSystemParameterDefinitionThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteSystemProperty(propertyKey: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      communityManager.deleteSystemParameterDefinitionThroughAutomationApi(communityKey, propertyKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateSystemProperty(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      communityManager.updateSystemParameterDefinitionThroughAutomationApi(communityKey, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createStatementProperty(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      parameterManager.createParameterDefinitionThroughAutomationApi(communityKey, actor, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteStatementProperty(actor: String, property: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageConfigurationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      parameterManager.deleteParameterDefinitionThroughAutomationApi(communityKey, actor, property).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateStatementProperty(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageConfigurationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCustomPropertyInfo(body)
      parameterManager.updateParameterDefinitionThroughAutomationApi(communityKey, actor, input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

}
