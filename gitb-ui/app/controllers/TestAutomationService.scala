package controllers

import com.fasterxml.jackson.core.JsonParseException
import controllers.util.{AuthorizedAction, RequestWithAttributes, ResponseConstructor}
import exceptions.{AutomationApiException, ErrorCodes, MissingRequiredParameterException}
import managers.{AuthorizationManager, TestExecutionManager}
import models.Constants
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import utils.JsonUtil

import javax.inject.{Inject, Singleton}

@Singleton
class TestAutomationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, authorizationManager: AuthorizationManager, testExecutionManager: TestExecutionManager) extends AbstractController(cc) {

  private val LOG = LoggerFactory.getLogger(classOf[TestAutomationService])

  private def bodyToJson(request: Request[AnyContent]): Option[JsValue] = {
    var result: Option[JsValue] = request.body.asJson
    if (result.isEmpty) {
      val text = request.body.asText
      if (text.isDefined) {
        result = Some(Json.parse(text.get))
      }
    }
    result
  }

  private def processAsJson(request: RequestWithAttributes[AnyContent], fn: JsValue => Result): Result = {
    authorizationManager.canManageSessionsThroughAutomationApi(request)
    var result: Result = null
    try {
      val json = bodyToJson(request)
      if (json.isEmpty) {
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Failed to parse provided payload as JSON")
      } else {
        result = fn(json.get)
      }
    } catch {
      case e: JsonParseException =>
        result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Failed to parse provided payload as JSON")
        LOG.warn("Failed to parse automation API payload: "+e.getMessage)
      case e: AutomationApiException =>
        result = ResponseConstructor.constructBadRequestResponse(e.getCode, e.getMessage)
        LOG.warn("Failure while processing automation API call: "+e.getMessage)
      case e: MissingRequiredParameterException =>
        result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.MISSING_REQUIRED_CONFIGURATION, e.getMessage)
        LOG.warn(e.getMessage)
      case e: Exception =>
        result = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Provided JSON payload was not defined as expected")
        LOG.warn("Unexpected error while processing automation API call: "+e.getMessage)
    }
    result
  }

  def start: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, { body =>
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      val input = JsonUtil.parseJsTestSessionLaunchRequest(body, organisationKey)
      val sessionIds = testExecutionManager.processAutomationLaunchRequest(input)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionLaunchInfo(sessionIds).toString())
    })
  }

  def stop: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, { body =>
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      val sessionIds = JsonUtil.parseJsSessions(body)
      testExecutionManager.processAutomationStopRequest(organisationKey, sessionIds)
      ResponseConstructor.constructEmptyResponse
    })
  }

  def status: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, { body =>
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      val query = JsonUtil.parseJsSessionStatusRequest(body)
      val sessionIds = query._1
      val withLogs = query._2
      val statusItems = testExecutionManager.processAutomationStatusRequest(organisationKey, sessionIds, withLogs)
      ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionStatusInfo(statusItems).toString())
    })
  }

}
