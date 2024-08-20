package controllers.rest

import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{ActorManager, AuthorizationManager, DomainManager, SpecificationManager}
import models.Constants
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class DomainAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                         cc: ControllerComponents,
                                         domainManager: DomainManager,
                                         specificationManager: SpecificationManager,
                                         actorManager: ActorManager,
                                         authorizationManager: AuthorizationManager) extends BaseAutomationService(cc) {

  def createDomain(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canCreateDomainThroughAutomationApi), { body =>
      val domain = JsonUtil.parseJsCreateDomainRequest(body).toDomain()
      val savedApiKey = domainManager.createDomain(domain, checkApiKeyUniqueness = true)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
    })
  }

  def deleteDomain(domain: String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canDeleteDomainThroughAutomationApi(request)
      domainManager.deleteDomainByApiKey(domain)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def updateDomain(domain: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canUpdateDomainThroughAutomationApi(request, Some(domain))
    processAsJson(request, None, { body =>
      val input = JsonUtil.parseJsUpdateDomainRequest(body, Some(domain), None)
      domainManager.updateDomainByApiKey(input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def updateDomainOfCommunity(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canUpdateDomainThroughAutomationApi(request, None)
    processAsJson(request, None, { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateDomainRequest(body, None, Some(communityKey))
      domainManager.updateDomainByApiKey(input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def createSpecificationGroup(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSpecificationGroupThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateSpecificationGroupRequest(body, communityKey)
      val savedApiKey = specificationManager.createSpecificationGroupThroughAutomationApi(input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
    })
  }

  def deleteSpecificationGroup(group: String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageSpecificationGroupThroughAutomationApi(request)
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      specificationManager.deleteSpecificationGroupThroughAutomationApi(group, communityKey)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def updateSpecificationGroup(group: String): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSpecificationGroupThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateSpecificationGroupRequest(body, group, communityKey)
      specificationManager.updateSpecificationGroupThroughAutomationApi(input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def createSpecification(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSpecificationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateSpecificationRequest(body, communityKey)
      val savedApiKey = specificationManager.createSpecificationThroughAutomationApi(input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
    })
  }

  def deleteSpecification(specification: String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageSpecificationThroughAutomationApi(request)
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      specificationManager.deleteSpecificationThroughAutomationApi(specification, communityKey)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def updateSpecification(specification: String): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSpecificationThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateSpecificationRequest(body, specification, communityKey)
      specificationManager.updateSpecificationThroughAutomationApi(input)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def createActor(): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageActorThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsCreateActorRequest(body, communityKey)
      val savedApiKey = actorManager.createActorThroughAutomationApi(input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
    })
  }

  def deleteActor(actor: String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageActorThroughAutomationApi(request)
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      actorManager.deleteActorThroughAutomationApi(actor, communityKey)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def updateActor(actor: String): Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageActorThroughAutomationApi), { body =>
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsUpdateActorRequest(body, actor, communityKey)
      actorManager.updateActorThroughAutomationApi(input)
      ResponseConstructor.constructEmptyResponse
    })
  }

}
