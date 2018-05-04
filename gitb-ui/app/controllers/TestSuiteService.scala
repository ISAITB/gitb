package controllers

import controllers.util.{ParameterExtractor, ResponseConstructor}
import managers.TestSuiteManager
import org.slf4j.{LoggerFactory, Logger}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import utils.JsonUtil


/**
 * Created by serbay.
 */
class TestSuiteService extends Controller {

	private final val logger: Logger = LoggerFactory.getLogger(classOf[TestSuiteService])

	def undeployTestSuite(testSuiteId: Long) = Action.apply {
		TestSuiteManager.undeployTestSuiteWrapper(testSuiteId)
		ResponseConstructor.constructEmptyResponse
	}

	def getTestSuites() = Action.apply { request =>
		val testSuiteIds = ParameterExtractor.extractLongIdsQueryParameter(request)

		val testSuites = TestSuiteManager.getTestSuites(testSuiteIds)
		val json = JsonUtil.jsTestSuitesList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def getTestSuitesWithTestCases() = Action.apply { request =>
		val testSuites = TestSuiteManager.getTestSuitesWithTestCases()
		val json = JsonUtil.jsTestSuiteList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

}