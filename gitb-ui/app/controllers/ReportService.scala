package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}

import javax.inject.Inject
import managers.{AuthorizationManager, CommunityManager, ReportManager}
import models.{Constants, OrganisationParameters, SystemParameters}
import play.api.mvc._
import utils.JsonUtil

/**
 * Created by senan on 04.12.2014.
 */
class ReportService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, reportManager: ReportManager, testService: TestService, authorizationManager: AuthorizationManager, communityManager: CommunityManager) extends AbstractController(cc) {

  def getSystemActiveTestResults = authorizedAction { request =>
    val organisationId = ParameterExtractor.requiredBodyParameter(request, Parameters.ORGANIZATION_ID).toLong

    authorizationManager.canViewTestResultsForOrganisation(request, organisationId)

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

    val testResultReports = reportManager.getOrganisationActiveTestResults(organisationId, systemIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, startTimeBegin, startTimeEnd, sessionId, sortColumn, sortOrder)
    val json = JsonUtil.jsTestResultReports(testResultReports, None).toString()
    ResponseConstructor.constructJsonResponse(json)

  }

  def getTestResults = authorizedAction { request =>
    val organisationId = ParameterExtractor.requiredBodyParameter(request, Parameters.ORGANIZATION_ID).toLong

    authorizationManager.canViewTestResultsForOrganisation(request, organisationId)

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

    val output = reportManager.getTestResults(page, limit, organisationId, systemIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, sortColumn, sortOrder)
    val json = JsonUtil.jsTestResultReports(output._1, Some(output._2)).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getActiveTestResults = authorizedAction { request =>
    val communityIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.COMMUNITY_IDS)

    authorizationManager.canViewTestResultsForCommunity(request, communityIds)

    val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
    val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.GROUP_IDS)
    val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
    val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_SUITE_IDS)
    val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
    val organizationIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS)
    val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_BEGIN)
    val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_END)
    val sessionId = ParameterExtractor.optionalBodyParameter(request, Parameters.SESSION_ID)
    val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
    val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)
    val forExport: Boolean = ParameterExtractor.optionalBodyParameter(request, Parameters.EXPORT).getOrElse("false").toBoolean
    val orgParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.ORGANISATION_PARAMETERS))
    val sysParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_PARAMETERS))

    val testResultReports = reportManager.getActiveTestResults(communityIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, startTimeBegin, startTimeEnd, sessionId, orgParameters, sysParameters, sortColumn, sortOrder)

    var orgParameterDefinitions: Option[List[OrganisationParameters]] = None
    var orgParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]] = None
    var sysParameterDefinitions: Option[List[SystemParameters]] = None
    var sysParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]] = None
    if (forExport && communityIds.isDefined && communityIds.get.size == 1) {
      orgParameterDefinitions = Some(communityManager.getSimpleOrganisationParameters(communityIds.get.head, Some(true)))
      orgParameterValues = Some(communityManager.getOrganisationParametersValuesForExport(communityIds.get.head, organizationIds))
      sysParameterDefinitions = Some(communityManager.getSimpleSystemParameters(communityIds.get.head, Some(true)))
      sysParameterValues = Some(communityManager.getSystemParametersValuesForExport(communityIds.get.head, organizationIds, systemIds))
    }
    val json = JsonUtil.jsTestResultSessionReports(testResultReports, orgParameterDefinitions, orgParameterValues, sysParameterDefinitions, sysParameterValues, None).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getFinishedTestResults = authorizedAction { request =>
    val communityIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.COMMUNITY_IDS)

    authorizationManager.canViewTestResultsForCommunity(request, communityIds)

    val page = getPageOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.PAGE))
    val limit = getLimitOrDefault(ParameterExtractor.optionalBodyParameter(request, Parameters.LIMIT))
    val domainIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SPEC_IDS)
    val specGroupIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.GROUP_IDS)
    val actorIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ACTOR_IDS)
    val testSuiteIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_SUITE_IDS)
    val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
    val organizationIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.SYSTEM_IDS)
    val results = ParameterExtractor.optionalListBodyParameter(request, Parameters.RESULTS)
    val startTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_BEGIN)
    val startTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.START_TIME_END)
    val endTimeBegin = ParameterExtractor.optionalBodyParameter(request, Parameters.END_TIME_BEGIN)
    val endTimeEnd = ParameterExtractor.optionalBodyParameter(request, Parameters.END_TIME_END)
    val sessionId = ParameterExtractor.optionalBodyParameter(request, Parameters.SESSION_ID)
    val sortColumn = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_COLUMN)
    val sortOrder = ParameterExtractor.optionalBodyParameter(request, Parameters.SORT_ORDER)
    val forExport: Boolean = ParameterExtractor.optionalBodyParameter(request, Parameters.EXPORT).getOrElse("false").toBoolean
    val orgParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.ORGANISATION_PARAMETERS))
    val sysParameters = JsonUtil.parseJsIdToValuesMap(ParameterExtractor.optionalBodyParameter(request, Parameters.SYSTEM_PARAMETERS))

    val output = reportManager.getFinishedTestResults(page, limit, communityIds, domainIds, specIds, specGroupIds, actorIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sessionId, orgParameters, sysParameters, sortColumn, sortOrder)

    var orgParameterDefinitions: Option[List[OrganisationParameters]] = None
    var orgParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]] = None
    var sysParameterDefinitions: Option[List[SystemParameters]] = None
    var sysParameterValues: Option[scala.collection.mutable.Map[Long, scala.collection.mutable.Map[Long, String]]] = None
    if (forExport && communityIds.isDefined && communityIds.get.size == 1) {
      orgParameterDefinitions = Some(communityManager.getSimpleOrganisationParameters(communityIds.get.head, Some(true)))
      orgParameterValues = Some(communityManager.getOrganisationParametersValuesForExport(communityIds.get.head, organizationIds))
      sysParameterDefinitions = Some(communityManager.getSimpleSystemParameters(communityIds.get.head, Some(true)))
      sysParameterValues = Some(communityManager.getSystemParametersValuesForExport(communityIds.get.head, organizationIds, systemIds))
    }
    val json = JsonUtil.jsTestResultSessionReports(output._1, orgParameterDefinitions, orgParameterValues, sysParameterDefinitions, sysParameterValues, Some(output._2)).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  private def getPageOrDefault(_page: Option[String] = None) = _page match {
    case Some(p) => p.toLong
    case None => Constants.defaultPage
  }

  private def getLimitOrDefault(_limit: Option[String] = None) = _limit match {
    case Some(l) => l.toLong
    case None => Constants.defaultLimit
  }

  def getTestResultOfSession(sessionId: String) = authorizedAction { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId)
    val response = reportManager.getTestResultOfSession(sessionId)
    val json = JsonUtil.jsTestResult(response._1, Some(response._2), withOutputMessage = true).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createTestReport() = authorizedAction { request =>
    val sessionId = ParameterExtractor.requiredBodyParameter(request, Parameters.SESSION_ID)
    authorizationManager.canViewTestResultForSession(request, sessionId)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val testId = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_ID)

    val response = testService.getTestCasePresentation(testId, Some(sessionId))
    reportManager.createTestReport(sessionId, systemId, testId, actorId, response.getTestcase)
    ResponseConstructor.constructEmptyResponse
  }

  def getTestStepResults(sessionId: String) = authorizedAction { request =>
    authorizationManager.canViewTestResultForSession(request, sessionId)
    val testStepResults = reportManager.getTestStepResults(sessionId)
    val json = JsonUtil.jsTestStepResults(testStepResults).toString()
    ResponseConstructor.constructJsonResponse(json)
  }
}
