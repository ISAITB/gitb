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

	def undeployTestSuite(testSuiteId: Long) = Action.async {
		TestSuiteManager.undeployTestSuite(testSuiteId) map { unit =>
			ResponseConstructor.constructEmptyResponse
		}
	}

	def getTestSuites() = Action.async { request =>
		val testSuiteIds = ParameterExtractor.extractLongIdsQueryParameter(request)

		TestSuiteManager.getTestSuites(testSuiteIds) map { testSuites =>
			val json = JsonUtil.jsTestSuites(testSuites).toString()
			ResponseConstructor.constructJsonResponse(json)
		}
	}
}
