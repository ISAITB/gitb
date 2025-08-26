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

import controllers.util.{AuthorizedAction, ParameterExtractor, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager, OrganizationManager, SystemManager}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CommunityAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                            cc: ControllerComponents,
                                            communityManager: CommunityManager,
                                            organisationManager: OrganizationManager,
                                            systemManager: SystemManager,
                                            authorizationManager: AuthorizationManager)
                                           (implicit ec: ExecutionContext) extends BaseAutomationService(cc) {

  def createCommunity(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canCreateCommunityThroughAutomationApi(request), { body =>
      val input = JsonUtil.parseJsCreateCommunityRequest(body)
      communityManager.createCommunityThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteCommunity(community: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canDeleteCommunityThroughAutomationApi(request), { _ =>
      communityManager.deleteCommunityThroughAutomationApi(community).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateCommunity(community: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageAnyCommunityThroughAutomationApi(request), { body =>
      val input = JsonUtil.parseJsUpdateCommunityRequest(body, community)
      communityManager.updateCommunityThroughAutomationApi(input, allowDomainChange = true).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateOwnCommunity(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageCommunityThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsUpdateCommunityRequest(body, communityKey)
      communityManager.updateCommunityThroughAutomationApi(input, allowDomainChange = false).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createOrganisation(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageOrganisationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCreateOrganisationRequest(body, communityKey)
      organisationManager.createOrganisationThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteOrganisation(organisation: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageOrganisationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      organisationManager.deleteOrganisationThroughAutomationApi(organisation, communityKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateOrganisation(organisation: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageOrganisationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsUpdateOrganisationRequest(body, organisation, communityKey)
      organisationManager.updateOrganisationThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createSystem(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageSystemThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCreateSystemRequest(body, communityKey)
      systemManager.createSystemThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteSystem(system: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageSystemThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      systemManager.deleteSystemThroughAutomationApi(system, communityKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateSystem(system: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageSystemThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsUpdateSystemRequest(body, system, communityKey)
      systemManager.updateSystemThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

}
