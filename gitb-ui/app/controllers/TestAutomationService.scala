package controllers

import controllers.util.{AuthorizedAction, ResponseConstructor}
import managers.{AuthorizationManager, TestExecutionManager}
import models.Constants
import play.api.mvc._
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class TestAutomationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, authorizationManager: AuthorizationManager, testExecutionManager: TestExecutionManager) extends BaseAutomationService(cc) {

  def start: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, authorizationManager.canManageSessionsThroughAutomationApi,
      { body =>
        val organisationKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSessionLaunchRequest(body, organisationKey)
        val sessionIds = testExecutionManager.processAutomationLaunchRequest(input)
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionLaunchInfo(sessionIds).toString())
      }
    )
  }

  def stop: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, authorizationManager.canManageSessionsThroughAutomationApi,
      { body =>
        val organisationKey = request.headers.get(Constants.AutomationHeader).get
        val sessionIds = JsonUtil.parseJsSessions(body)
        testExecutionManager.processAutomationStopRequest(organisationKey, sessionIds)
        ResponseConstructor.constructEmptyResponse
      }
    )
  }

  def status: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, authorizationManager.canManageSessionsThroughAutomationApi,
      { body =>
        val organisationKey = request.headers.get(Constants.AutomationHeader).get
        val query = JsonUtil.parseJsSessionStatusRequest(body)
        val sessionIds = query._1
        val withLogs = query._2
        val statusItems = testExecutionManager.processAutomationStatusRequest(organisationKey, sessionIds, withLogs)
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionStatusInfo(statusItems).toString())
      }
    )
  }

}
