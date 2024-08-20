package controllers.rest

import controllers.util.{AuthorizedAction, RequestWithAttributes, ResponseConstructor}
import managers.{AuthorizationManager, ReportManager, SystemManager, TestExecutionManager}
import models.{Constants, SessionFolderInfo}
import org.apache.commons.io.FileUtils
import play.api.http.MimeTypes
import play.api.mvc._
import utils.{JsonUtil, RepositoryUtils}

import java.nio.file.{Path, Paths}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TestAutomationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, reportManager: ReportManager,  repositoryUtils: RepositoryUtils, authorizationManager: AuthorizationManager, systemManager: SystemManager, testExecutionManager: TestExecutionManager) extends BaseAutomationService(cc) {

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

  def report(sessionId: String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canOrganisationUseAutomationApi(request)
    var reportData: Option[(Path, SessionFolderInfo)] = None
    try {
      val organisationKey = request.headers.get(Constants.AutomationHeader).get
      reportData = testExecutionManager.processAutomationReportRequest(organisationKey, sessionId)
      if (reportData.isDefined) {
        Ok.sendFile(
          content = reportData.get._1.toFile,
          onClose = () => {
            if (reportData.get._2.archived) {
              FileUtils.deleteQuietly(reportData.get._2.path.toFile)
            }
          }
        ).as(MimeTypes.XML)
      } else {
        NotFound
      }
    } catch {
      case e: Throwable =>
        if (reportData.isDefined && reportData.get._2.archived) {
          FileUtils.deleteQuietly(reportData.get._2.path.toFile)
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
    Paths.get(
      repositoryUtils.getTempReportFolder().getAbsolutePath,
      UUID.randomUUID().toString+suffix
    )
  }

}
