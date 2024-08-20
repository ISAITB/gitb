package controllers.rest

import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{AuthorizationManager, DomainManager, SpecificationManager}
import models.Constants
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class DomainAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                         cc: ControllerComponents,
                                         domainManager: DomainManager,
                                         specificationManager: SpecificationManager,
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
      val savedApiKey = specificationManager.createSpecificationGroupViaAutomationApi(input)
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
      val savedApiKey = specificationManager.createSpecificationViaAutomationApi(input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsApiKey(savedApiKey).toString())
    })
  }

  def deleteSpecification(specification: String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canManageSpecificationThroughAutomationApi(request)
      val communityKey = request.headers.get(Constants.AutomationHeader).get
      specificationManager.deleteSpecificationViaAutomationApi(specification, communityKey)
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

}
