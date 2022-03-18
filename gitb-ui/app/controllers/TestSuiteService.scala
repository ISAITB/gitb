package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import managers._
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.JsonUtil

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import utils.HtmlUtil

/**
 * Created by serbay.Tes
 */
class TestSuiteService @Inject() (implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, testSuiteManager: TestSuiteManager, testCaseManager: TestCaseManager, specificationManager: SpecificationManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager) extends AbstractController(cc) {

	private final val logger: Logger = LoggerFactory.getLogger(classOf[TestSuiteService])

	def updateTestSuiteMetadata(testSuiteId:Long) = authorizedAction { request =>
		authorizationManager.canEditTestSuite(request, testSuiteId)
		val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
		val version:String = ParameterExtractor.requiredBodyParameter(request, Parameters.VERSION)
		val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
		var documentation:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DOCUMENTATION)
		if (documentation.isDefined) {
			documentation = Some(HtmlUtil.sanitizeEditorContent(documentation.get))
		}
		testSuiteManager.updateTestSuiteMetadata(testSuiteId, name, description, documentation, version)
		ResponseConstructor.constructEmptyResponse
	}

	def updateTestCaseMetadata(testCaseId:Long) = authorizedAction { request =>
		authorizationManager.canEditTestCase(request, testCaseId)
		val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
		val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
		var documentation:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DOCUMENTATION)
		if (documentation.isDefined) {
			documentation = Some(HtmlUtil.sanitizeEditorContent(documentation.get))
		}
		testSuiteManager.updateTestCaseMetadata(testCaseId, name, description, documentation)
		ResponseConstructor.constructEmptyResponse
	}

	def undeployTestSuite(testSuiteId: Long) = authorizedAction { request =>
		authorizationManager.canDeleteTestSuite(request, testSuiteId)
		conformanceManager.undeployTestSuiteWrapper(testSuiteId)
		ResponseConstructor.constructEmptyResponse
	}

	def searchTestSuites() = authorizedAction { request =>
		authorizationManager.canViewAllTestSuites(request)
		val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
		val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
		val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
		val results = testSuiteManager.searchTestSuites(domainIds, specificationIds, actorIds)
		val json = JsonUtil.jsTestSuiteList(results).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def searchTestSuitesInDomain() = authorizedAction { request =>
		val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
		authorizationManager.canViewDomains(request, Some(List(domainId)))
		val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
		val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
		val results = testSuiteManager.searchTestSuites(Some(List(domainId)), specificationIds, actorIds)
		val json = JsonUtil.jsTestSuiteList(results).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def getTestSuiteWithTestCases(testSuiteId: Long) = authorizedAction { request =>
		authorizationManager.canViewTestSuite(request, testSuiteId)

		val testSuite = testSuiteManager.getTestSuiteWithTestCases(testSuiteId)
		val json = JsonUtil.jsTestSuite(testSuite, withDocumentation = true).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def getTestCase(testCaseId: Long) = authorizedAction { request =>
		authorizationManager.canViewTestCase(request, testCaseId.toString)

		val testCase = testCaseManager.getTestCaseWithDocumentation(testCaseId)
		val json = JsonUtil.jsTestCases(testCase, withDocumentation = true).toString()
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