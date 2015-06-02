package controllers

import com.gitb.tpl.ObjectFactory
import com.gitb.utils.XMLUtils
import controllers.util.{Parameters, ParameterExtractor, ResponseConstructor}
import managers.ReportManager
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc._
import utils.JsonUtil

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by senan on 04.12.2014.
 */
class ReportService extends Controller{
  private final val logger: Logger = LoggerFactory.getLogger(classOf[ReportService])

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

  def getTestResultOfSession(sessionId:String) = Action.async { request =>
      ReportManager.getTestResultOfSession(sessionId) map { response =>
      val json = JsonUtil.jsTestResult(response, true).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }

  def createTestReport() = Action.async { request =>
    val sessionId = ParameterExtractor.requiredBodyParameter(request, Parameters.SESSION_ID)
    val systemId  = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId   = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val testId    = ParameterExtractor.requiredBodyParameter(request, Parameters.TEST_ID)

    Future {
      val response = TestService.getTestCasePresentation(testId)
      val presentation = XMLUtils.marshalToString(new ObjectFactory().createTestcase(response.getTestcase))

      ReportManager.createTestReport(sessionId, systemId, testId, actorId, presentation)
    } map { response =>
      ResponseConstructor.constructEmptyResponse
    }
  }

  def getTestStepResults(sessionId:String) = Action.async { request =>
    ReportManager.getTestStepResults(sessionId) map {testStepResults =>
      val json = JsonUtil.jsTestStepResults(testStepResults).toString()
      ResponseConstructor.constructJsonResponse(json)
    }
  }
}
