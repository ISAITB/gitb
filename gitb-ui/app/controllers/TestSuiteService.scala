package controllers

import java.nio.file.Paths

import controllers.util.{AuthorizedAction, ResponseConstructor}
import javax.inject.Inject
import managers._
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Controller
import utils.{JsonUtil, RepositoryUtils, ZipArchiver}


/**
 * Created by serbay.Tes
 */
class TestSuiteService @Inject() (testSuiteManager: TestSuiteManager, specificationManager: SpecificationManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager) extends Controller {

	private final val logger: Logger = LoggerFactory.getLogger(classOf[TestSuiteService])

	def undeployTestSuite(testSuiteId: Long) = AuthorizedAction { request =>
		authorizationManager.canDeleteTestSuite(request, testSuiteId)
		conformanceManager.undeployTestSuiteWrapper(testSuiteId)
		ResponseConstructor.constructEmptyResponse
	}

	def getAllTestSuitesWithTestCases() = AuthorizedAction { request =>
		authorizationManager.canViewAllTestSuites(request)

		val testSuites = testSuiteManager.getTestSuitesWithTestCases()
		val json = JsonUtil.jsTestSuiteList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def getTestSuitesWithTestCasesForCommunity(communityId: Long) = AuthorizedAction { request =>
		authorizationManager.canViewTestSuitesByCommunityId(request, communityId)

		val testSuites = testSuiteManager.getTestSuitesWithTestCasesForCommunity(communityId)
		val json = JsonUtil.jsTestSuiteList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def getTestSuitesWithTestCasesForSystem(systemId: Long) = AuthorizedAction { request =>
		authorizationManager.canViewTestSuitesBySystemId(request, systemId)

		val testSuites = testSuiteManager.getTestSuitesWithTestCasesForSystem(systemId)
		val json = JsonUtil.jsTestSuiteList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def downloadTestSuite(testSuiteId: Long) = AuthorizedAction { request =>
		authorizationManager.canDownloadTestSuite(request, testSuiteId)
		val testSuite = testSuiteManager.getTestSuites(Some(List(testSuiteId))).head
		val testSuiteFolder = RepositoryUtils.getTestSuitesResource(specificationManager.getSpecificationById(testSuite.specification), testSuite.shortname)
		val testSuiteOutputPath = Paths.get(
			ReportManager.getTempFolderPath().toFile.getAbsolutePath,
			"test_suite",
			"test_suite."+testSuiteId.toString+"."+System.currentTimeMillis()+".zip"
		)
		val zipArchiver = new ZipArchiver(testSuiteFolder.toPath, testSuiteOutputPath)
		zipArchiver.zip()

		Ok.sendFile(
			content = testSuiteOutputPath.toFile,
			inline = false,
			fileName = _ => testSuiteOutputPath.toFile.getName,
			onClose = () => FileUtils.deleteQuietly(testSuiteOutputPath.toFile)
		)
	}

}