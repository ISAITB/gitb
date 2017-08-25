package controllers

import com.gitb.tpl.ObjectFactory
import com.gitb.utils.XMLUtils
import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import managers.ReportManager
import models.TestResultSessionReport
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
/*import utils.JsonUtil*/

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import utils.{JsonUtil, JacksonUtil, TimeUtil}

/**
 * Created by senan on 04.12.2014.
 */
class ReportService extends Controller {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ReportService])

  val defaultPage = 1L
  val defaultLimit = 10L

  def getTestResults = Action.async { request =>
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val limit = ParameterExtractor.optionalQueryParameter(request, Parameters.LIMIT) match {
      case Some(limitStr) => Some(limitStr.toLong)
      case None => None
    }
    val page = ParameterExtractor.optionalQueryParameter(request, Parameters.PAGE) match {
      case Some(pageStr) => Some(pageStr.toLong)
      case None => None
    }

    ReportManager.getTestResults(systemId, page, limit) map { testResultReports =>
      val json = JsonUtil.jsTestResultReports(testResultReports).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  def getActiveTestResults = Action.async { request =>
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

    ReportManager.getActiveTestResults(domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, startTimeBegin, startTimeEnd, sortColumn, sortOrder) map { testResultReports =>
      val json = JsonUtil.jsTestResultSessionReports(testResultReports).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  def getFinishedTestResultsCount = Action.async { request =>
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

    ReportManager.getFinishedTestResultsCount(domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd) map { count =>
      val json = JsonUtil.jsCount(count).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  def getFinishedTestResults = Action.async { request =>
    val page = getPageOrDefault(ParameterExtractor.optionalQueryParameter(request, Parameters.PAGE))
    val limit = getLimitOrDefault(ParameterExtractor.optionalQueryParameter(request, Parameters.LIMIT))
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

    ReportManager.getFinishedTestResults(page, limit, domainIds, specIds, testSuiteIds, testCaseIds, organizationIds, systemIds, results, startTimeBegin, startTimeEnd, endTimeBegin, endTimeEnd, sortColumn, sortOrder) map { testResultReports =>
      val json = JsonUtil.jsTestResultSessionReports(testResultReports).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  private def getPageOrDefault(_page: Option[String] = None) = _page match {
    case Some(p) => p.toLong
    case None => defaultPage
  }

  private def getLimitOrDefault(_limit: Option[String] = None) = _limit match {
    case Some(l) => l.toLong
    case None => defaultLimit
  }

  def getTestResultOfSession(sessionId: String) = Action.async { request =>
    ReportManager.getTestResultOfSession(sessionId) map { response =>
      val json = JsonUtil.jsTestResult(response, true).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  def createTestReport() = Action.async { request =>
    val sessionId = ParameterExtractor.requiredBodyParameter(request, Parameters.SESSION_ID)
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val testId = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_ID)

    Future {
      val response = TestService.getTestCasePresentation(testId)
      val presentation = XMLUtils.marshalToString(new ObjectFactory().createTestcase(response.getTestcase))

      ReportManager.createTestReport(sessionId, systemId, testId, actorId, presentation)
    } map { response =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getTestStepResults(sessionId: String) = Action.async { request =>
    ReportManager.getTestStepResults(sessionId) map { testStepResults =>
      val json = JsonUtil.jsTestStepResults(testStepResults).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }
}
