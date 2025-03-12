package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, Parameters, ResponseConstructor}
import managers._
import org.apache.commons.io.FileUtils
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{HtmlUtil, JsonUtil, RepositoryUtils}

import java.nio.file.Paths
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestSuiteService @Inject() (authorizedAction: AuthorizedAction,
																	cc: ControllerComponents,
																	specificationManager: SpecificationManager,
																	repositoryUtils: RepositoryUtils,
																	reportManager: ReportManager,
																	communityManager: CommunityManager,
																	testSuiteManager: TestSuiteManager,
																	testCaseManager: TestCaseManager,
																	authorizationManager: AuthorizationManager)
																 (implicit ec: ExecutionContext) extends AbstractController(cc) {

	def updateTestSuiteMetadata(testSuiteId:Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canEditTestSuite(request, testSuiteId).flatMap { _ =>
			val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
			val version:String = ParameterExtractor.requiredBodyParameter(request, Parameters.VERSION)
			val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
			val specReference = ParameterExtractor.optionalBodyParameter(request, Parameters.SPEC_REFERENCE)
			val specDescription = ParameterExtractor.optionalBodyParameter(request, Parameters.SPEC_DESCRIPTION)
			val specLink = ParameterExtractor.optionalBodyParameter(request, Parameters.SPEC_LINK)
			var documentation:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DOCUMENTATION)
			if (documentation.isDefined) {
				documentation = Some(HtmlUtil.sanitizeEditorContent(documentation.get))
			}
			testSuiteManager.updateTestSuiteMetadata(testSuiteId, name, description, documentation, version, specReference, specDescription, specLink).map { _ =>
				ResponseConstructor.constructEmptyResponse
			}
		}
	}

	def updateTestCaseMetadata(testCaseId:Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canEditTestCase(request, testCaseId).flatMap { _ =>
			val name:String = ParameterExtractor.requiredBodyParameter(request, Parameters.NAME)
			val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DESCRIPTION)
			var documentation:Option[String] = ParameterExtractor.optionalBodyParameter(request, Parameters.DOCUMENTATION)
			val isOptional = ParameterExtractor.optionalBooleanBodyParameter(request, Parameters.OPTIONAL).getOrElse(false)
			val isDisabled = ParameterExtractor.optionalBooleanBodyParameter(request, Parameters.DISABLED).getOrElse(false)
			val tags = sanitizeTags(ParameterExtractor.optionalBodyParameter(request, Parameters.TAGS))
			val specReference = ParameterExtractor.optionalBodyParameter(request, Parameters.SPEC_REFERENCE)
			val specDescription = ParameterExtractor.optionalBodyParameter(request, Parameters.SPEC_DESCRIPTION)
			val specLink = ParameterExtractor.optionalBodyParameter(request, Parameters.SPEC_LINK)
			if (documentation.isDefined) {
				documentation = Some(HtmlUtil.sanitizeEditorContent(documentation.get))
			}
			testCaseManager.updateTestCaseMetadata(testCaseId, name, description, documentation, isOptional, isDisabled, tags, specReference, specDescription, specLink).map { _ =>
				ResponseConstructor.constructEmptyResponse
			}
		}
	}

	private def sanitizeTags(tagDefinition: Option[String]): Option[String] = {
		if (tagDefinition.isDefined) {
			val parsedTags = JsonUtil.parseJsTags(tagDefinition.get)
			if (parsedTags.nonEmpty) {
				Some(JsonUtil.jsTags(parsedTags).toString)
			} else {
				None
			}
		} else {
			None
		}
	}

	def previewTestCaseDocumentationInReports(): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canPreviewDocumentation(request).flatMap { _ =>
			val userId = ParameterExtractor.extractUserId(request)
			val documentation = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, Parameters.DOCUMENTATION))
			communityManager.getUserCommunityId(userId).map { communityId =>
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
		}
	}

	def undeployTestSuite(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canDeleteTestSuite(request, testSuiteId).flatMap { _ =>
			testSuiteManager.undeployTestSuiteWrapper(testSuiteId).map { _ =>
				ResponseConstructor.constructEmptyResponse
			}
		}
	}

	def searchTestSuites(): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canViewAllTestSuites(request).flatMap { _ =>
			val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.DOMAIN_IDS)
			val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
			val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
			val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
			testSuiteManager.searchTestSuites(domainIds, specificationIds, groupIds, actorIds).map { results =>
				val json = JsonUtil.jsTestSuiteList(results).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
		}
	}

	def searchTestSuitesInDomain(): Action[AnyContent] = authorizedAction.async { request =>
		val domainId = ParameterExtractor.requiredBodyParameter(request, Parameters.DOMAIN_ID).toLong
		authorizationManager.canViewDomains(request, Some(List(domainId))).flatMap { _ =>
			val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.SPEC_IDS)
			val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.GROUP_IDS)
			val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, Parameters.ACTOR_IDS)
			testSuiteManager.searchTestSuites(Some(List(domainId)), specificationIds, groupIds, actorIds).map { results =>
				val json = JsonUtil.jsTestSuiteList(results).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
		}
	}

	def getTestSuiteWithTestCases(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canViewTestSuite(request, testSuiteId).flatMap { _ =>
			testSuiteManager.getTestSuiteWithTestCaseData(testSuiteId).map { testSuite =>
				val json = JsonUtil.jsTestSuite(testSuite, withDocumentation = true, withSpecReference = true, withGroups = true, withTags = true).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
		}
	}

	def getTestCase(testCaseId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canViewTestCase(request, testCaseId.toString).flatMap { _ =>
			testCaseManager.getTestCaseWithDocumentation(testCaseId).map { testCase =>
				val json = JsonUtil.jsTestCases(testCase, withDocumentation = true, withTags = true, withSpecReference = true).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
		}
	}

	def downloadTestSuite(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canDownloadTestSuite(request, testSuiteId).flatMap { _ =>
			testSuiteManager.getTestSuites(Some(List(testSuiteId))).map { testSuites =>
				val testSuite = testSuites.head
				val testSuiteOutputPath = testSuiteManager.extractTestSuite(testSuite, None)
				Ok.sendFile(
					content = testSuiteOutputPath.toFile,
					inline = false,
					fileName = _ => Some(testSuiteOutputPath.toFile.getName),
					onClose = () => FileUtils.deleteQuietly(testSuiteOutputPath.toFile)
				)
			}
		}
	}

	def getLinkedSpecifications(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canManageTestSuite(request, testSuiteId).flatMap { _ =>
			specificationManager.getSpecificationsLinkedToTestSuite(testSuiteId).map { specs =>
				val json = JsonUtil.jsSpecifications(specs).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
		}
	}

}