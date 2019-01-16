package controllers

import java.nio.file.Paths

import controllers.util.{ParameterExtractor, ResponseConstructor}
import javax.inject.Inject
import managers.{ConformanceManager, ReportManager, SpecificationManager, TestSuiteManager}
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{Action, Controller}
import utils.{JsonUtil, RepositoryUtils, ZipArchiver}


/**
 * Created by serbay.Tes
 */
class TestSuiteService @Inject() (testSuiteManager: TestSuiteManager, specificationManager: SpecificationManager, conformanceManager: ConformanceManager) extends Controller {

	private final val logger: Logger = LoggerFactory.getLogger(classOf[TestSuiteService])

	def undeployTestSuite(testSuiteId: Long) = Action.apply {
		conformanceManager.undeployTestSuiteWrapper(testSuiteId)
		ResponseConstructor.constructEmptyResponse
	}

	def getTestSuites() = Action.apply { request =>
		val testSuiteIds = ParameterExtractor.extractLongIdsQueryParameter(request)

		val testSuites = testSuiteManager.getTestSuites(testSuiteIds)
		val json = JsonUtil.jsTestSuitesList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def getTestSuitesWithTestCases() = Action.apply { request =>
		val testSuites = testSuiteManager.getTestSuitesWithTestCases()
		val json = JsonUtil.jsTestSuiteList(testSuites).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def downloadTestSuite(testSuiteId: Long) = Action.apply {
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