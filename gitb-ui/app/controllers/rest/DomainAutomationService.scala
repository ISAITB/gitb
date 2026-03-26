/*
 * Copyright (C) 2026 European Union
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

import controllers.rest.BaseAutomationService.{DeleteEndpoint, GetEndpoint, PostEndpoint, PutEndpoint}
import controllers.util.{AuthorizedAction, ParameterExtractor, ParameterNames, ResponseConstructor}
import managers.ratelimit.RateLimitManager
import managers.{ActorManager, AuthorizationManager, DomainManager, SpecificationManager}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DomainAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                         cc: ControllerComponents,
                                         domainManager: DomainManager,
                                         specificationManager: SpecificationManager,
                                         actorManager: ActorManager,
                                         rateLimitManager: RateLimitManager,
                                         authorizationManager: AuthorizationManager)
                                        (implicit ec: ExecutionContext) extends BaseAutomationService(cc, rateLimitManager) {

  def searchDomains(): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/domains"), () => authorizationManager.canViewDomainsThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val name = ParameterExtractor.optionalQueryParameter(request, ParameterNames.NAME)
      domainManager.searchDomainsThroughAutomationApi(communityKey, name).map { domains =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsDomainsForAutomationApi(domains).toString())
      }
    })
  }

  def getDomainOfCommunity(): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/domain"), () => authorizationManager.canViewDomainThroughAutomationApi(request, None), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      domainManager.getCommunityDomainThroughAutomationApi(communityKey).map { domain =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsDomainForAutomationApi(domain).toString())
      }
    })
  }

  def getDomain(domain: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/domain/{domain}"), () => authorizationManager.canViewDomainThroughAutomationApi(request, Some(domain)), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      domainManager.getDomainThroughAutomationApi(communityKey, domain).map { domain =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsDomainForAutomationApi(domain).toString())
      }
    })
  }

  def createDomain(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PutEndpoint("/domain"), () => authorizationManager.canCreateDomainThroughAutomationApi(request), { body =>
      val domain = JsonUtil.parseJsCreateDomainRequest(body).toDomain()
      domainManager.createDomain(domain, checkApiKeyUniqueness = true).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteDomain(domain: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, DeleteEndpoint("/domain/{domain}"), () => authorizationManager.canDeleteDomainThroughAutomationApi(request), { _ =>
      domainManager.deleteDomainThroughAutomationApi(domain).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateDomain(domain: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PostEndpoint("/domain/{domain}"), () => authorizationManager.canUpdateDomainThroughAutomationApi(request, Some(domain)), { body =>
      val input = JsonUtil.parseJsUpdateDomainRequest(body, Some(domain), None)
      domainManager.updateDomainThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateDomainOfCommunity(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PostEndpoint("/domain"), () => authorizationManager.canUpdateDomainThroughAutomationApi(request, None), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsUpdateDomainRequest(body, None, Some(communityKey))
      domainManager.updateDomainThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def getSpecificationGroup(group: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/group/{group}"), () => authorizationManager.canManageSpecificationGroupThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      specificationManager.getSpecificationGroupThroughAutomationApi(communityKey, group).map { group =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroupForAutomationApi(group).toString())
      }
    })
  }

  def searchSpecificationGroups(domain: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/domain/{domain}/groups"), () => authorizationManager.canManageSpecificationGroupThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val name = ParameterExtractor.optionalQueryParameter(request, ParameterNames.NAME)
      specificationManager.searchSpecificationGroupsThroughAutomationApi(communityKey, domain, name).map { groups =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationGroupsForAutomationApi(groups).toString())
      }
    })
  }

  def createSpecificationGroup(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PutEndpoint("/group"), () => authorizationManager.canManageSpecificationGroupThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCreateSpecificationGroupRequest(body, communityKey)
      specificationManager.createSpecificationGroupThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteSpecificationGroup(group: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, DeleteEndpoint("/group/{group}"), () => authorizationManager.canManageSpecificationGroupThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      specificationManager.deleteSpecificationGroupThroughAutomationApi(group, communityKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateSpecificationGroup(group: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PostEndpoint("/group/{group}"), () => authorizationManager.canManageSpecificationGroupThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsUpdateSpecificationGroupRequest(body, group, communityKey)
      specificationManager.updateSpecificationGroupThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def getSpecification(specification: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/specification/{specification}"), () => authorizationManager.canManageSpecificationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      specificationManager.getSpecificationThroughAutomationApi(communityKey, specification).map { specification =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationForAutomationApi(specification).toString())
      }
    })
  }

  def searchSpecifications(domain: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/domain/{domain}/specifications"), () => authorizationManager.canManageSpecificationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val name = ParameterExtractor.optionalQueryParameter(request, ParameterNames.NAME)
      specificationManager.searchSpecificationsThroughAutomationApi(communityKey, domain, name).map { specifications =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationsForAutomationApi(specifications).toString())
      }
    })
  }

  def searchSpecificationsInGroup(group: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/group/{group}/specifications"), () => authorizationManager.canManageSpecificationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val name = ParameterExtractor.optionalQueryParameter(request, ParameterNames.NAME)
      specificationManager.searchSpecificationsInGroupThroughAutomationApi(communityKey, group, name).map { specifications =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsSpecificationsForAutomationApi(specifications).toString())
      }
    })
  }

  def createSpecification(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PutEndpoint("/specification"), () => authorizationManager.canManageSpecificationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCreateSpecificationRequest(body, communityKey)
      specificationManager.createSpecificationThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteSpecification(specification: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, DeleteEndpoint("/specification/{specification}"), () => authorizationManager.canManageSpecificationThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      specificationManager.deleteSpecificationThroughAutomationApi(specification, communityKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateSpecification(specification: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PostEndpoint("/specification/{specification}"), () => authorizationManager.canManageSpecificationThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsUpdateSpecificationRequest(body, specification, communityKey)
      specificationManager.updateSpecificationThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def getActor(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/actor/{actor}"), () => authorizationManager.canManageActorThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      actorManager.getActorThroughAutomationApi(communityKey, actor).map { actor =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsActorForAutomationApi(actor).toString())
      }
    })
  }

  def searchActors(specification: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/specification/{specification}/actors"), () => authorizationManager.canManageActorThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val name = ParameterExtractor.optionalQueryParameter(request, ParameterNames.NAME)
      actorManager.searchActorsThroughAutomationApi(communityKey, specification, name).map { actors =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsActorsForAutomationApi(actors).toString())
      }
    })
  }

  def createActor(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PutEndpoint("/actor"), () => authorizationManager.canManageActorThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsCreateActorRequest(body, communityKey)
      actorManager.createActorThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteActor(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, DeleteEndpoint("/actor/{actor}"), () => authorizationManager.canManageActorThroughAutomationApi(request), { _ =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      actorManager.deleteActorThroughAutomationApi(actor, communityKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateActor(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PostEndpoint("/actor/{actor}"), () => authorizationManager.canManageActorThroughAutomationApi(request), { body =>
      val communityKey = ParameterExtractor.extractApiKeyHeader(request).get
      val input = JsonUtil.parseJsUpdateActorRequest(body, actor, communityKey)
      actorManager.updateActorThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

}
