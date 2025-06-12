package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import managers.{AuthorizationManager, CommunityManager, ReportManager, TestResultManager}
import models.Constants
import play.api.mvc._
import utils.JsonUtil

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by senan on 04.12.2014.
 */
class ReportService @Inject() (authorizedAction: AuthorizedAction,
                               cc: ControllerComponents,
                               reportManager: ReportManager,
                               testResultManager: TestResultManager,
                               testService: TestService,
                               authorizationManager: AuthorizationManager,
                               communityManager: CommunityManager)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getSystemActiveTestResults: Action[AnyContent] = authorizedAction.async { request =>
    val organisationId = ParameterExtractor.requiredBodyParameter(request, Parameters.ORGANIZATION_ID).toLong

    authorizationManager.canViewTestResultsForOrganisation(request, organisationId).flatMap { _ =>
      val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.PAGE))
      val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.LIMIT))
      val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS)
      val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
      val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
      val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.GROUP_IDS)
      val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
      val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_SUITE_IDS)
      val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
      val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_BEGIN)
      val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_END)
      val sessionId = ParameterExtractor.optionalBodyParameter(request, Parameters.SESSION_ID)
      val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
      val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)

      testResultManager.getOrganisationActiveTestResults(page, limit, organisationId, systemIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, startTimeBegin, startTimeEnd, sessionId, sortColumn, sortOrder).map { output =>
        val json = JsonUtil.jsTestResultReports(output._1, Some(output._2)).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getTestResults: Action[AnyContent] = authorizedAction.async { request =>
    val organisationId = ParameterExtractor.requiredBodyParameter(request, Parameters.ORGANIZATION_ID).toLong

    authorizationManager.canViewTestResultsForOrganisation(request, organisationId).flatMap { _ =>
      val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.PAGE))
      val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.LIMIT))
      val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS)
      val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
      val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
      val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.GROUP_IDS)
      val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
      val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_SUITE_IDS)
      val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
      val results = ParameterExtractor.optionalListBodyParameter(request, Parameters.RESULTS)
      val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_BEGIN)
      val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_END)
      val endTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.END_TIME_BEGIN)
      val endTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.END_TIME_END)
      val sessionId = ParameterExtractor.optionalBodyParameter(request, Parameters.SESSION_ID)
      val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
      val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)

      testResultManager.getTestResults(page, limit, organisationId, systemIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, sortColumn, sortOrder).map { output =>
        val json = JsonUtil.jsTestResultReports(output._1, Some(output._2)).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getActiveTestResults: Action[AnyContent] = authorizedAction.async { request =>
    val communityIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.COMMUNITY_IDS)
    val organizationIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS)
    for {
      _ <- authorizationManager.canViewActiveTestsForCommunity(request, communityIds)
      results <- {
        val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.PAGE))
        val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.LIMIT))
        val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
        val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
        val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.GROUP_IDS)
        val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
        val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_SUITE_IDS)
        val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
        val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_BEGIN)
        val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_END)
        val sessionId = ParameterExtractor.optionalBodyParameter(request, Parameters.SESSION_ID)
        val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
        val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)
        val orgParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.ORGANISATION_PARAMETERS))
        val sysParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_PARAMETERS))
        val pendingAdminInteraction = ParameterExtractor.optionalBodyParameter(request, Parameters.PENDING_ADMIN_INTERACTION).exists(_.toBoolean)
        testResultManager.getActiveTestResults(page, limit, communityIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, startTimeBegin, startTimeEnd, sessionId, orgParameters, sysParameters, sortColumn, sortOrder, pendingAdminInteraction)
      }
      parameterInfo <- {
        val forExport = ParameterExtractor.optionalBodyParameter(request, Parameters.EXPORT).getOrElse("false").toBoolean
        if (forExport && communityIds.isDefined && communityIds.get.size == 1) {
          communityManager.getParameterInfo(communityIds.get.head, organizationIds, systemIds).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      result <- {
        val json = JsonUtil.jsTestResultSessionReports(results._1, parameterInfo, Some(results._2)).toString()
        Future.successful {
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    } yield result
  }

  def getFinishedTestResults: Action[AnyContent] = authorizedAction.async { request =>
    val communityIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.COMMUNITY_IDS)
    val organizationIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS)
    for {
      _ <- authorizationManager.canViewCommunityTests(request, communityIds)
      results <- {
        val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.PAGE))
        val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.LIMIT))
        val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
        val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
        val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.GROUP_IDS)
        val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
        val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_SUITE_IDS)
        val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
        val results = ParameterExtractor.optionalListBodyParameter(request, Parameters.RESULTS)
        val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_BEGIN)
        val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_END)
        val endTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.END_TIME_BEGIN)
        val endTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.END_TIME_END)
        val sessionId = ParameterExtractor.optionalBodyParameter(request, Parameters.SESSION_ID)
        val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
        val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)
        val orgParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.ORGANISATION_PARAMETERS))
        val sysParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_PARAMETERS))
        testResultManager.getFinishedTestResults(page, limit, communityIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, orgParameters, sysParameters, sortColumn, sortOrder)
      }
      parameterInfo <- {
        val forExport = ParameterExtractor.optionalBodyParameter(request, Parameters.EXPORT).getOrElse("false").toBoolean
        if (forExport && communityIds.isDefined && communityIds.get.size == 1) {
          communityManager.getParameterInfo(communityIds.get.head, organizationIds, systemIds).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      result <- {
        val json = JsonUtil.jsTestResultSessionReports(results._1, parameterInfo, Some(results._2)).toString()
        Future.successful{
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    } yield result
  }

  private def getPageOrDefault(_page: Option[String] = None) = _page match {
    case Some(p) => p.toLong
    case None => Constants.defaultPage
  }

  private def getLimitOrDefault(_limit: Option[String] = None) = _limit match {
    case Some(l) => l.toLong
    case None => Constants.defaultLimit
  }

  def getTestResultOfSession(sessionId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      testResultManager.getTestResultOfSession(sessionId).map { response =>
        val json = JsonUtil.jsTestResult(response._1, Some(response._2), withOutputMessage = true).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def createTestReport(): Action[AnyContent] = authorizedAction.async { request =>
    val sessionId = ParameterExtractor.requiredBodyParameter(request, Parameters.SESSION_ID)
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
      val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
      val testId = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_ID)
      testService.getTestCasePresentationByStatement(testId, Some(sessionId), actorId, systemId).flatMap { response =>
        reportManager.createTestReport(sessionId, systemId, testId, actorId, response.getTestcase).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def getTestStepResults(sessionId: String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      reportManager.getTestStepResults(sessionId).map { testStepResults =>
        val json = JsonUtil.jsTestStepResults(testStepResults).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }
}
