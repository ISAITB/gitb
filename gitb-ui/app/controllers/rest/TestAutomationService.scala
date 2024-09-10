package controllers.rest

import controllers.util.{AuthorizedAction, RequestWithAttributes, ResponseConstructor}
import exceptions.{AutomationApiException, ErrorCodes}
import managers.{AuthorizationManager, ReportManager, SystemManager, TestExecutionManager}
import models.Constants
import org.apache.commons.io.FileUtils
import play.api.http.MimeTypes
import play.api.mvc._
import utils.{JsonUtil, RepositoryUtils}

import java.nio.file.{Files, Path}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TestAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                       cc: ControllerComponents,
                                       reportManager: ReportManager,
                                       repositoryUtils: RepositoryUtils,
                                       authorizationManager: AuthorizationManager,
                                       systemManager: SystemManager,
                                       testExecutionManager: TestExecutionManager) extends BaseAutomationService(cc) {

  def start: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canOrganisationUseAutomationApi),
      { body =>
        val organisationKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSessionLaunchRequest(body, organisationKey)
        val sessionIds = testExecutionManager.processAutomationLaunchRequest(input)
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionLaunchInfo(sessionIds).toString())
      }
    )
  }

  def stop: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canOrganisationUseAutomationApi),
      { body =>
        val organisationKey = request.headers.get(Constants.AutomationHeader).get
        val sessionIds = JsonUtil.parseJsSessions(body)
        testExecutionManager.processAutomationStopRequest(organisationKey, sessionIds)
        ResponseConstructor.constructEmptyResponse
      }
    )
  }

  def status: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canOrganisationUseAutomationApi),
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

  private def determineReportType(request: RequestWithAttributes[AnyContent]): String = {
    request.headers.get(Constants.AcceptHeader)
      .map(x => {
        if (x == Constants.MimeTypeTextXML || x == Constants.MimeTypeAny) {
          Constants.MimeTypeXML
        } else if (x != Constants.MimeTypeXML && x != Constants.MimeTypePDF) {
          throw AutomationApiException(ErrorCodes.INVALID_REQUEST, "Unsupported report type [%s] requested through %s header".formatted(x, Constants.AcceptHeader))
        } else {
          x
        }
      }).getOrElse(Constants.MimeTypeXML)
  }

  def report(sessionId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canOrganisationUseAutomationApi(request)
    var report: Option[Path] = None
    try {
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      val contentType = determineReportType(request)
      val suffix = if (contentType == Constants.MimeTypePDF) ".pdf" else ".xml"
      report = testExecutionManager.processAutomationReportRequest(getReportTempFile(suffix), organisationKey, sessionId, contentType)
      if (report.isDefined) {
        Ok.sendFile(
          content = report.get.toFile,
          fileName = _ => Some("test_report"+suffix),
          onClose = () => FileUtils.deleteQuietly(report.get.toFile)
        ).as(contentType)
      } else {
        NotFound
      }
    } catch {
      case e: Throwable =>
        if (report.exists(Files.exists(_))) {
          FileUtils.deleteQuietly(report.get.toFile)
        }
        handleException(e)
    }
  }

  def createStatement(systemKey: String, actorKey: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canOrganisationUseAutomationApi(request)
    try {
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      systemManager.defineConformanceStatementViaApi(organisationKey, systemKey, actorKey)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def deleteStatement(systemKey: String, actorKey: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canOrganisationUseAutomationApi(request)
    try {
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      systemManager.deleteConformanceStatementViaApi(organisationKey, systemKey, actorKey)
      ResponseConstructor.constructEmptyResponse
    } catch {
      case e: Throwable => handleException(e)
    }
  }

  def statementReport(systemKey: String, actorKey: String): Action[AnyContent] = authorizedAction { request =>
    statementReportInternal(systemKey, actorKey, None, request)
  }

  def statementReportForSnapshot(systemKey: String, actorKey: String, snapshotKey: String): Action[AnyContent] = authorizedAction { request =>
    statementReportInternal(systemKey, actorKey, Some(snapshotKey), request)
  }

  private def statementReportInternal(systemKey: String, actorKey: String, snapshotKey: Option[String], request: RequestWithAttributes[AnyContent]): Result = {
    authorizationManager.canOrganisationUseAutomationApi(request)
    var report: Option[Path] = None
    try {
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      val reportPath = getReportTempFile(".xml")
      report = Some(reportManager.generateConformanceStatementReportInXMLViaApi(reportPath, organisationKey, systemKey, actorKey, snapshotKey))
      Ok.sendFile(
        content = report.get.toFile,
        onClose = () => FileUtils.deleteQuietly(report.get.toFile)
      ).as(MimeTypes.XML)
    } catch {
      case e: Throwable =>
        if (report.isDefined) {
          FileUtils.deleteQuietly(report.get.toFile)
        }
        handleException(e)
    }
  }

  private def getReportTempFile(suffix: String): Path = {
    repositoryUtils.getReportTempFile(suffix)
  }

}
