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

package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, ParameterNames, ResponseConstructor}
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
    val organisationId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ORGANIZATION_ID).toLong

    authorizationManager.canViewTestResultsForOrganisation(request, organisationId).flatMap { _ =>
      val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, ParameterNames.PAGE))
      val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, ParameterNames.LIMIT))
      val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.SYSTEM_IDS)
      val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.DOMAIN_IDS)
      val specIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.SPEC_IDS)
      val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.GROUP_IDS)
      val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.ACTOR_IDS)
      val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.TEST_SUITE_IDS)
      val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.TEST_CASE_IDS)
      val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, ParameterNames.START_TIME_BEGIN)
      val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, ParameterNames.START_TIME_END)
      val sessionId = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SESSION_ID)
      val sortColumn = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SORT_COLUMN)
      val sortOrder = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SORT_ORDER)

      testResultManager.getOrganisationActiveTestResults(page, limit, organisationId, systemIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, startTimeBegin, startTimeEnd, sessionId, sortColumn, sortOrder).map { output =>
        val json = JsonUtil.jsSearchResult(output, JsonUtil.jsTestResultReports).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getTestResults: Action[AnyContent] = authorizedAction.async { request =>
    val organisationId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ORGANIZATION_ID).toLong

    authorizationManager.canViewTestResultsForOrganisation(request, organisationId).flatMap { _ =>
      val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, ParameterNames.PAGE))
      val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, ParameterNames.LIMIT))
      val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.SYSTEM_IDS)
      val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.DOMAIN_IDS)
      val specIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.SPEC_IDS)
      val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.GROUP_IDS)
      val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.ACTOR_IDS)
      val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.TEST_SUITE_IDS)
      val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.TEST_CASE_IDS)
      val results = ParameterExtractor.optionalListBodyParameter(request, ParameterNames.RESULTS)
      val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, ParameterNames.START_TIME_BEGIN)
      val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, ParameterNames.START_TIME_END)
      val endTimeBegin = ParameterExtractor.optionalBodyParameter(request, ParameterNames.END_TIME_BEGIN)
      val endTimeEnd = ParameterExtractor.optionalBodyParameter(request, ParameterNames.END_TIME_END)
      val sessionId = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SESSION_ID)
      val sortColumn = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SORT_COLUMN)
      val sortOrder = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SORT_ORDER)

      testResultManager.getTestResults(page, limit, organisationId, systemIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, sortColumn, sortOrder).map { output =>
        val json = JsonUtil.jsSearchResult(output, JsonUtil.jsTestResultReports).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  def getActiveTestResults: Action[AnyContent] = authorizedAction.async { request =>
    val communityIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.COMMUNITY_IDS)
    val organizationIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.SYSTEM_IDS)
    for {
      _ <- authorizationManager.canViewActiveTestsForCommunity(request, communityIds)
      results <- {
        val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, ParameterNames.PAGE))
        val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, ParameterNames.LIMIT))
        val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.DOMAIN_IDS)
        val specIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.SPEC_IDS)
        val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.GROUP_IDS)
        val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.ACTOR_IDS)
        val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.TEST_SUITE_IDS)
        val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.TEST_CASE_IDS)
        val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, ParameterNames.START_TIME_BEGIN)
        val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, ParameterNames.START_TIME_END)
        val sessionId = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SESSION_ID)
        val sortColumn = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SORT_COLUMN)
        val sortOrder = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SORT_ORDER)
        val orgParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, ParameterNames.ORGANISATION_PARAMETERS))
        val sysParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, ParameterNames.SYSTEM_PARAMETERS))
        val pendingAdminInteraction = ParameterExtractor.optionalBodyParameter(request, ParameterNames.PENDING_ADMIN_INTERACTION).exists(_.toBoolean)
        testResultManager.getActiveTestResults(page, limit, communityIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, startTimeBegin, startTimeEnd, sessionId, orgParameters, sysParameters, sortColumn, sortOrder, pendingAdminInteraction)
      }
      parameterInfo <- {
        val forExport = ParameterExtractor.optionalBodyParameter(request, ParameterNames.EXPORT).getOrElse("false").toBoolean
        if (forExport && communityIds.isDefined && communityIds.get.size == 1) {
          communityManager.getParameterInfo(communityIds.get.head, organizationIds, systemIds).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      result <- {
        val json = JsonUtil.jsTestResultSessionReports(results, parameterInfo).toString()
        Future.successful {
          ResponseConstructor.constructJsonResponse(json)
        }
      }
    } yield result
  }

  def getFinishedTestResults: Action[AnyContent] = authorizedAction.async { request =>
    val communityIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.COMMUNITY_IDS)
    val organizationIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.SYSTEM_IDS)
    for {
      _ <- authorizationManager.canViewCommunityTests(request, communityIds)
      results <- {
        val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, ParameterNames.PAGE))
        val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, ParameterNames.LIMIT))
        val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.DOMAIN_IDS)
        val specIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.SPEC_IDS)
        val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.GROUP_IDS)
        val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.ACTOR_IDS)
        val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.TEST_SUITE_IDS)
        val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, ParameterNames.TEST_CASE_IDS)
        val results = ParameterExtractor.optionalListBodyParameter(request, ParameterNames.RESULTS)
        val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, ParameterNames.START_TIME_BEGIN)
        val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, ParameterNames.START_TIME_END)
        val endTimeBegin = ParameterExtractor.optionalBodyParameter(request, ParameterNames.END_TIME_BEGIN)
        val endTimeEnd = ParameterExtractor.optionalBodyParameter(request, ParameterNames.END_TIME_END)
        val sessionId = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SESSION_ID)
        val sortColumn = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SORT_COLUMN)
        val sortOrder = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SORT_ORDER)
        val orgParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, ParameterNames.ORGANISATION_PARAMETERS))
        val sysParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, ParameterNames.SYSTEM_PARAMETERS))
        testResultManager.getFinishedTestResults(page, limit, communityIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, orgParameters, sysParameters, sortColumn, sortOrder)
      }
      parameterInfo <- {
        val forExport = ParameterExtractor.optionalBodyParameter(request, ParameterNames.EXPORT).getOrElse("false").toBoolean
        if (forExport && communityIds.isDefined && communityIds.get.size == 1) {
          communityManager.getParameterInfo(communityIds.get.head, organizationIds, systemIds).map(Some(_))
        } else {
          Future.successful(None)
        }
      }
      result <- {
        val json = JsonUtil.jsTestResultSessionReports(results, parameterInfo).toString()
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
    val sessionId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.SESSION_ID)
    authorizationManager.canViewTestResultForSession(request, sessionId).flatMap { _ =>
      val systemId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.SYSTEM_ID).toLong
      val actorId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.ACTOR_ID).toLong
      val testId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.TEST_ID)
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
