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

import config.Configurations
import controllers.rest.BaseAutomationService.{DeleteEndpoint, EndpointSignature, GetEndpoint, PostEndpoint, PutEndpoint}
import controllers.util.{AuthorizedAction, ParameterExtractor, RequestWithAttributes, ResponseConstructor}
import exceptions.{AutomationApiException, ErrorCodes}
import managers.ratelimit.RateLimitManager
import managers.{AuthorizationManager, ReportManager, SystemManager, TestExecutionManager}
import models.Constants
import org.apache.commons.io.FileUtils
import play.api.mvc._
import utils.{JsonUtil, RepositoryUtils}

import java.nio.file.Path
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                       cc: ControllerComponents,
                                       reportManager: ReportManager,
                                       repositoryUtils: RepositoryUtils,
                                       authorizationManager: AuthorizationManager,
                                       systemManager: SystemManager,
                                       rateLimitManager: RateLimitManager,
                                       testExecutionManager: TestExecutionManager)
                                      (implicit ec: ExecutionContext) extends BaseAutomationService(cc, rateLimitManager) {

  def start: Action[AnyContent] = authorizedAction.async { request =>
    if (Configurations.PREPARE_FOR_SHUTDOWN) {
      Future.successful {
        authorizationManager.markRequestAsAuthorized(request)
        ResponseConstructor.constructErrorResponse(ErrorCodes.PREPARING_FOR_SHUTDOWN, "The Test Bed is preparing to be shut down and will not accept new test sessions.")
      }
    } else {
      processAsJson(request, GetEndpoint("/tests/start"), () => authorizationManager.canOrganisationUseAutomationApi(request), { body =>
        val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
        val input = JsonUtil.parseJsTestSessionLaunchRequest(body, organisationKey)
        if (!rateLimitManager.currentSettings().enableBulkTestExecution && input.testCase.size != 1) {
          Future.successful {
            ResponseConstructor.constructErrorResponse(ErrorCodes.INVALID_REQUEST, "Bulk test execution is not allowed. To launch tests you must provide the identifier of a single, specific test case to execute.")
          }
        } else {
          testExecutionManager.processAutomationLaunchRequest(input).map { result =>
            ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionLaunchInfo(result).toString())
          }
        }
      })
    }
  }

  def stop: Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PostEndpoint("/tests/stop"), () => authorizationManager.canOrganisationUseAutomationApi(request), { body =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      val sessionIds = JsonUtil.parseJsSessions(body)
      testExecutionManager.processAutomationStopRequest(organisationKey, sessionIds).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def status: Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, PostEndpoint("/tests/status"), () => authorizationManager.canOrganisationUseAutomationApi(request), { body =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      val query = JsonUtil.parseJsSessionStatusRequest(body)
      val sessionIds = query._1
      val withLogs = query._2
      val withReports = query._3
      reportManager.processAutomationStatusRequest(organisationKey, sessionIds, withLogs, withReports).map { statusItems =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSessionStatusInfo(statusItems).toString())
      }
    })
  }

  def report(sessionId: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/tests/report/{sessionId}"), () => authorizationManager.canOrganisationUseAutomationApi(request), { _ =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      val contentType = determineReportType(request)
      val suffix = if (contentType == Constants.MimeTypePDF) ".pdf" else ".xml"
      reportManager.processAutomationReportRequest(getReportTempFile(suffix), organisationKey, sessionId, contentType).map { report =>
        if (report.isDefined) {
          Ok.sendFile(
            content = report.get.toFile,
            fileName = _ => Some("test_report"+suffix),
            onClose = () => FileUtils.deleteQuietly(report.get.toFile)
          ).as(contentType)
        } else {
          NotFound
        }
      }
    })
  }

  def getOrganisationStatements(): Action[AnyContent] = authorizedAction.async { request =>
    process(request, PostEndpoint("/conformance"), () => authorizationManager.canOrganisationUseAutomationApi(request), { _ =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      systemManager.getConformanceStatementsViaApi(organisationKey, None, None).map { result =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsStatementsForAutomationApi(result).toString())
      }
    })
  }

  def getOrganisationStatementsOfSnapshot(snapshotKey: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/conformance/snapshot/{snapshot}"), () => authorizationManager.canOrganisationUseAutomationApi(request), { _ =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      systemManager.getConformanceStatementsViaApi(organisationKey, None, Some(snapshotKey)).map { result =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsStatementsForAutomationApi(result).toString())
      }
    })
  }

  def getSystemStatements(systemKey: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/conformance/{system}"), () => authorizationManager.canOrganisationUseAutomationApi(request), { _ =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      systemManager.getConformanceStatementsViaApi(organisationKey, Some(systemKey), None).map { result =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsStatementsForAutomationApi(result).toString())
      }
    })
  }

  def getSystemStatementsOfSnapshot(systemKey: String, snapshotKey: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, GetEndpoint("/conformance/snapshot/{snapshot}/{system}"), () => authorizationManager.canOrganisationUseAutomationApi(request), { _ =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      systemManager.getConformanceStatementsViaApi(organisationKey, Some(systemKey), Some(snapshotKey)).map { result =>
        ResponseConstructor.constructJsonResponse(JsonUtil.jsStatementsForAutomationApi(result).toString())
      }
    })
  }

  def createStatement(systemKey: String, actorKey: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, PutEndpoint("/conformance/{system}/{actor}"), () => authorizationManager.canOrganisationUseAutomationApi(request), { _ =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      systemManager.defineConformanceStatementViaApi(organisationKey, systemKey, actorKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def deleteStatement(systemKey: String, actorKey: String): Action[AnyContent] = authorizedAction.async { request =>
    process(request, DeleteEndpoint("/conformance/{system}/{actor}"), () => authorizationManager.canOrganisationUseAutomationApi(request), { _ =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      systemManager.deleteConformanceStatementViaApi(organisationKey, systemKey, actorKey).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    })
  }

  def statementReport(systemKey: String, actorKey: String): Action[AnyContent] = authorizedAction.async { request =>
    statementReportInternal(GetEndpoint("/conformance/{system}/{actor}"), systemKey, actorKey, None, request)
  }

  def statementReportForSnapshot(systemKey: String, actorKey: String, snapshotKey: String): Action[AnyContent] = authorizedAction.async { request =>
    statementReportInternal(GetEndpoint("/conformance/{system}/{actor}/{snapshot}"), systemKey, actorKey, Some(snapshotKey), request)
  }

  private def statementReportInternal(signature: EndpointSignature, systemKey: String, actorKey: String, snapshotKey: Option[String], request: RequestWithAttributes[AnyContent]): Future[Result] = {
    process(request, signature, () => authorizationManager.canOrganisationUseAutomationApi(request), { _ =>
      val organisationKey = ParameterExtractor.extractApiKeyHeader(request).get
      val contentType = determineReportType(request)
      val suffix = if (contentType == Constants.MimeTypePDF) ".pdf" else ".xml"
      reportManager.generateConformanceStatementReportViaApi(getReportTempFile(suffix), organisationKey, systemKey, actorKey, snapshotKey, contentType).map { report =>
        Ok.sendFile(
          content = report.toFile,
          fileName = _ => Some("conformance_report"+suffix),
          onClose = () => FileUtils.deleteQuietly(report.toFile)
        ).as(contentType)
      }
    })
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

  private def getReportTempFile(suffix: String): Path = {
    repositoryUtils.getReportTempFile(suffix)
  }

}
