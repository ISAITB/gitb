package controllers

import controllers.util.{AuthorizedAction, ResponseConstructor}
import javax.inject.Inject
import managers._
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.JsonUtil

import scala.concurrent.ExecutionContext


/**
 * Created by serbay.Tes
 */
class TestSuiteService @Inject() (implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, testSuiteManager: TestSuiteManager, specificationManager: SpecificationManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager) extends AbstractController(cc) {

	private final val logger: Logger = LoggerFactory.getLogger(classOf[TestSuiteService])

	def undeployTestSuite(testSuiteId: Long) = authorizedAction { request =>
		authorizationManager.canDeleteTestSuite(request, testSuiteId)
		conformanceManager.undeployTestSuiteWrapper(testSuiteId)
		ResponseConstructor.constructEmptyResponse
	}

	def getAllTestSuitesWithTestCases() = authorizedAction { request =>
		authorizationManager.canViewAllTestSuites(request)

		val testSuites = testSuiteManager.getTestSuitesWithTestCases()
		val json = JsonUtil.jsTestSuiteList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def getTestSuitesWithTestCasesForCommunity(communityId: Long) = authorizedAction { request =>
		authorizationManager.canViewTestSuitesByCommunityId(request, communityId)

		val testSuites = testSuiteManager.getTestSuitesWithTestCasesForCommunity(communityId)
		val json = JsonUtil.jsTestSuiteList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def getTestSuitesWithTestCasesForSystem(systemId: Long) = authorizedAction { request =>
		authorizationManager.canViewTestSuitesBySystemId(request, systemId)

		val testSuites = testSuiteManager.getTestSuitesWithTestCasesForSystem(systemId)
		val json = JsonUtil.jsTestSuiteList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def downloadTestSuite(testSuiteId: Long) = authorizedAction { request =>
		authorizationManager.canDownloadTestSuite(request, testSuiteId)
		val testSuite = testSuiteManager.getTestSuites(Some(List(testSuiteId))).head
		val testSuiteOutputPath = testSuiteManager.extractTestSuite(testSuite, specificationManager.getSpecificationById(testSuite.specification), None)
		Ok.sendFile(
			content = testSuiteOutputPath.toFile,
			inline = false,
			fileName = _ => Some(testSuiteOutputPath.toFile.getName),
			onClose = () => FileUtils.deleteQuietly(testSuiteOutputPath.toFile)
		)
	}

}