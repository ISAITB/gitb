package controllers

import controllers.util.{AuthorizedAction, ResponseConstructor}
import exceptions.{AutomationApiException, ErrorCodes}
import managers.{AuthorizationManager, TestExecutionManager}
import models.Constants
import org.slf4j.LoggerFactory
import play.api.http.MimeTypes
import play.api.mvc._
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class TestAutomationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, authorizationManager: AuthorizationManager, testExecutionManager: TestExecutionManager) extends BaseAutomationService(cc) {

  private val LOG = LoggerFactory.getLogger(classOf[TestAutomationService])

  def start: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSessionsThroughAutomationApi),
      { body =>
        val organisationKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSessionLaunchRequest(body, organisationKey)
        val sessionIds = testExecutionManager.processAutomationLaunchRequest(input)
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionLaunchInfo(sessionIds).toString())
      }
    )
  }

  def stop: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSessionsThroughAutomationApi),
      { body =>
        val organisationKey = request.headers.get(Constants.AutomationHeader).get
        val sessionIds = JsonUtil.parseJsSessions(body)
        testExecutionManager.processAutomationStopRequest(organisationKey, sessionIds)
        ResponseConstructor.constructEmptyResponse
      }
    )
  }

  def status: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageSessionsThroughAutomationApi),
      { body =>
        val organisationKey = request.headers.get(Constants.AutomationHeader).get
        val query = JsonUtil.parseJsSessionStatusRequest(body)
        val sessionIds = query._1
        val withLogs = query._2
        val withReports = query._3
        val statusItems = testExecutionManager.processAutomationStatusRequest(organisationKey, sessionIds, withLogs, withReports)
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionStatusInfo(statusItems).toString())
      }
    )
  }

  def report(sessionId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageSessionsThroughAutomationApi(request)
    try {
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      val report = testExecutionManager.processAutomationReportRequest(organisationKey, sessionId)
      if (report.isDefined) {
        Ok(report.get).as(MimeTypes.XML)
      } else {
        NotFound
      }
    } catch {
      case e: AutomationApiException =>
        LOG.warn("Failure while processing automation API call: " + e.getMessage)
        ResponseConstructor.constructBadRequestResponse(e.getCode, e.getMessage)
      case e: Exception =>
        LOG.warn("Unexpected error while processing automation API call: " + e.getMessage, e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "An unexpected error occurred while processing your request")
    }
  }

}
