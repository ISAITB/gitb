package controllers

import com.gitb.tpl.ObjectFactory
import com.gitb.utils.XMLUtils
import controllers.util.{ParameterExtractor, Parameters, ResponseConstructor}
import javax.inject.Inject
import managers.ReportManager
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc._
import utils.JsonUtil

/**
 * Created by senan on 04.12.2014.
 */
class ReportService @Inject() (reportManager: ReportManager, testService: TestService) extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ReportService])

  val defaultPage = 1L
  val defaultLimit = 10L

  def getTestResults = Action.apply { request =>
    val page = getPageOrDefault(ParameterExtractor.optionalQueryParameter(request, Parameters.PAGE))
    val limit = getLimitOrDefault(ParameterExtractor.optionalQueryParameter(request, Parameters.LIMIT))
    val domainIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.DOMAIN_IDS)
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val specIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SPEC_IDS)
    val testSuiteIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_SUITE_IDS)
    val testCaseIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_CASE_IDS)
    val results = ParameterExtractor.optionalListQueryParameter(request, Parameters.RESULTS)
    val startTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_BEGIN)
    val startTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_END)
    val endTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.END_TIME_BEGIN)
    val endTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.END_TIME_END)
    val sortColumn = ParameterExtractor.optionalQueryParameter(request, Parameters.SORT_COLUMN)
    val sortOrder = ParameterExtractor.optionalQueryParameter(request, Parameters.SORT_ORDER)

    val testResultReports = reportManager.getTestResults(page, limit, systemId, domainIds, specIds, testSuiteIds, testCaseIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder)
    val json = JsonUtil.jsTestResultReports(testResultReports).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getTestResultsCount = Action.apply { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val domainIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SPEC_IDS)
    val testSuiteIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_SUITE_IDS)
    val testCaseIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_CASE_IDS)
    val results = ParameterExtractor.optionalListQueryParameter(request, Parameters.RESULTS)
    val startTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_BEGIN)
    val startTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_END)
    val endTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.END_TIME_BEGIN)
    val endTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.END_TIME_END)

    val count = reportManager.getTestResultsCount(systemId, domainIds, specIds, testSuiteIds, testCaseIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd)
    val json = JsonUtil.jsCount(count).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getActiveTestResults = Action.apply { request =>
    val communityIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.COMMUNITY_IDS)
    val domainIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SPEC_IDS)
    val testSuiteIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_SUITE_IDS)
    val testCaseIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_CASE_IDS)
    val organizationIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SYSTEM_IDS)
    val startTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_BEGIN)
    val startTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_END)
    val sortColumn = ParameterExtractor.optionalQueryParameter(request, Parameters.SORT_COLUMN)
    val sortOrder = ParameterExtractor.optionalQueryParameter(request, Parameters.SORT_ORDER)

    val testResultReports = reportManager.getActiveTestResults(communityIds, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, startTimeBegin, startTimeEnd, sortColumn, sortOrder)
    val json = JsonUtil.jsTestResultSessionReports(testResultReports).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getFinishedTestResultsCount = Action.apply { request =>
    val communityIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.COMMUNITY_IDS)
    val domainIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SPEC_IDS)
    val testSuiteIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_SUITE_IDS)
    val testCaseIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_CASE_IDS)
    val organizationIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SYSTEM_IDS)
    val results = ParameterExtractor.optionalListQueryParameter(request, Parameters.RESULTS)
    val startTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_BEGIN)
    val startTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_END)
    val endTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.END_TIME_BEGIN)
    val endTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.END_TIME_END)

    val count = reportManager.getFinishedTestResultsCount(communityIds, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd)
    val json = JsonUtil.jsCount(count).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def getFinishedTestResults = Action.apply { request =>
    val page = getPageOrDefault(ParameterExtractor.optionalQueryParameter(request, Parameters.PAGE))
    val limit = getLimitOrDefault(ParameterExtractor.optionalQueryParameter(request, Parameters.LIMIT))
    val communityIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.COMMUNITY_IDS)
    val domainIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.DOMAIN_IDS)
    val specIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SPEC_IDS)
    val testSuiteIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_SUITE_IDS)
    val testCaseIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.TEST_CASE_IDS)
    val organizationIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.ORG_IDS)
    val systemIds = ParameterExtractor.optionalLongListQueryParameter(request, Parameters.SYSTEM_IDS)
    val results = ParameterExtractor.optionalListQueryParameter(request, Parameters.RESULTS)
    val startTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_BEGIN)
    val startTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.START_TIME_END)
    val endTimeBegin = ParameterExtractor.optionalQueryParameter(request, Parameters.END_TIME_BEGIN)
    val endTimeEnd = ParameterExtractor.optionalQueryParameter(request, Parameters.END_TIME_END)
    val sortColumn = ParameterExtractor.optionalQueryParameter(request, Parameters.SORT_COLUMN)
    val sortOrder = ParameterExtractor.optionalQueryParameter(request, Parameters.SORT_ORDER)

    val testResultReports = reportManager.getFinishedTestResults(page, limit, communityIds, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder)
    val json = JsonUtil.jsTestResultSessionReports(testResultReports).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  private def getPageOrDefault(_page: Option[String] = None) = _page match {
    case Some(p) => p.toLong
    case None => defaultPage
  }

  private def getLimitOrDefault(_limit: Option[String] = None) = _limit match {
    case Some(l) => l.toLong
    case None => defaultLimit
  }

  def getTestResultOfSession(sessionId: String) = Action.apply { request =>
    val response = reportManager.getTestResultOfSession(sessionId)
    val json = JsonUtil.jsTestResult(response, true).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  def createTestReport() = Action.apply { request =>
    val sessionId = ParameterExtractor.requiredBodyParameter(request, Parameters.SESSION_ID)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val testId = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_ID)

    val response = testService.getTestCasePresentation(testId)
    val presentation = XMLUtils.marshalToString(new ObjectFactory().createTestcase(response.getTestcase))
    reportManager.createTestReport(sessionId, systemId, testId, actorId, presentation)
    ResponseConstructor.constructEmptyResponse
  }

  def getTestStepResults(sessionId: String) = Action.apply { request =>
    val testStepResults = reportManager.getTestStepResults(sessionId)
    val json = JsonUtil.jsTestStepResults(testStepResults).toString()
    ResponseConstructor.constructJsonResponse(json)
  }
}
