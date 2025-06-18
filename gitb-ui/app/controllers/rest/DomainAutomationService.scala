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

import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{ActorManager, AuthorizationManager, DomainManager, SpecificationManager}
import models.Constants
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
                                         authorizationManager: AuthorizationManager)
                                        (implicit ec: ExecutionContext) extends BaseAutomationService(cc) {

  def createDomain(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canCreateDomainThroughAutomationApi(request), { body =>
      val domain = JsonUtil.parseJsCreateDomainRequest(body).toDomain()
      domainManager.createDomain(domain, checkApiKeyUniqueness = true).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteDomain(domain: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canDeleteDomainThroughAutomationApi(request), { _ =>
      domainManager.deleteDomainThroughAutomationApi(domain).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateDomain(domain: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canUpdateDomainThroughAutomationApi(request, Some(domain)), { body =>
      val input = JsonUtil.parseJsUpdateDomainRequest(body, Some(domain), None)
      domainManager.updateDomainThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateDomainOfCommunity(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canUpdateDomainThroughAutomationApi(request, None), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateDomainRequest(body, None, Some(communityKey))
      domainManager.updateDomainThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createSpecificationGroup(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageSpecificationGroupThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateSpecificationGroupRequest(body, communityKey)
      specificationManager.createSpecificationGroupThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteSpecificationGroup(group: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageSpecificationGroupThroughAutomationApi(request), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      specificationManager.deleteSpecificationGroupThroughAutomationApi(group, communityKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateSpecificationGroup(group: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageSpecificationGroupThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateSpecificationGroupRequest(body, group, communityKey)
      specificationManager.updateSpecificationGroupThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createSpecification(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageSpecificationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateSpecificationRequest(body, communityKey)
      specificationManager.createSpecificationThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteSpecification(specification: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageSpecificationThroughAutomationApi(request), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      specificationManager.deleteSpecificationThroughAutomationApi(specification, communityKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateSpecification(specification: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageSpecificationThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateSpecificationRequest(body, specification, communityKey)
      specificationManager.updateSpecificationThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def createActor(): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageActorThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateActorRequest(body, communityKey)
      actorManager.createActorThroughAutomationApi(input).map { savedApiKey =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
      }
    })
  }

  def deleteActor(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    process(() => authorizationManager.canManageActorThroughAutomationApi(request), { _ =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      actorManager.deleteActorThroughAutomationApi(actor, communityKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def updateActor(actor: String): Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageActorThroughAutomationApi(request), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateActorRequest(body, actor, communityKey)
      actorManager.updateActorThroughAutomationApi(input).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

}
