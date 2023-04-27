package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import managers._
import org.apache.commons.io.FileUtils
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.{HtmlUtil, JsonUtil, RepositoryUtils}

import java.nio.file.Paths
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

/**
 * Created by serbay.Tes
 */
class TestSuiteService @Inject() (implicit ec: ExecutionContext, authorizedAction: AuthorizedAction, cc: ControllerComponents, repositoryUtils: RepositoryUtils, reportManager: ReportManager, communityManager: CommunityManager, testSuiteManager: TestSuiteManager, testCaseManager: TestCaseManager, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager) extends AbstractController(cc) {

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
		testCaseManager.updateTestCaseMetadata(testCaseId, name, description, documentation)
		ResponseConstructor.constructEmptyResponse
	}

	def previewTestCaseDocumentationInReports() = authorizedAction { request =>
		authorizationManager.canPreviewDocumentation(request)
		val userId = ParameterExtractor.extractUserId(request)
		val documentation = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.DOCUMENTATION))
		val communityId = communityManager.getUserCommunityId(userId)
		val reportPath = Paths.get(
			repositoryUtils.getTempReportFolder().getAbsolutePath,
			"reports",
			"preview_"+UUID.randomUUID().toString+".pdf"
		)
		try {
			reportManager.generateTestCaseDocumentationPreviewReport(reportPath, communityId, documentation)
			Ok.sendFile(
				content = reportPath.toFile,
				fileName = _ => Some("report_preview.pdf"),
				onClose = () => {
					FileUtils.deleteQuietly(reportPath.toFile)
				}
			)
		} catch {
			case e: Exception =>
				FileUtils.deleteQuietly(reportPath.toFile)
				throw e
		}
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
		val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
		val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
		val results = testSuiteManager.searchTestSuites(domainIds, specificationIds, groupIds, actorIds)
		val json = JsonUtil.jsTestSuiteList(results).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

	def searchTestSuitesInDomain() = authorizedAction { request =>
		val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
		authorizationManager.canViewDomains(request, Some(List(domainId)))
		val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
		val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
		val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
		val results = testSuiteManager.searchTestSuites(Some(List(domainId)), specificationIds, groupIds, actorIds)
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
		val testSuiteOutputPath = testSuiteManager.extractTestSuite(testSuite, None)
		Ok.sendFile(
			content = testSuiteOutputPath.toFile,
			inline = false,
			fileName = _ => Some(testSuiteOutputPath.toFile.getName),
			onClose = () => FileUtils.deleteQuietly(testSuiteOutputPath.toFile)
		)
	}

	def getLinkedSpecifications(testSuiteId: Long) = authorizedAction { request =>
		authorizationManager.canManageTestSuite(request, testSuiteId)
		val specs = testSuiteManager.getLinkedSpecifications(testSuiteId)
		val json = JsonUtil.jsSpecifications(specs).toString()
		ResponseConstructor.constructJsonResponse(json)
	}

}