/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package controllers

import controllers.util.{AuthorizedAction, ParameterExtractor, ParameterNames, ResponseConstructor}
import exceptions.{ErrorCodes, UserException}
import managers._
import models.Enums.LabelType
import org.apache.commons.io.FileUtils
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import utils.{HtmlUtil, JsonUtil, RepositoryUtils}

import java.nio.file.Paths
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestSuiteService @Inject() (authorizedAction: AuthorizedAction,
																	cc: ControllerComponents,
																	specificationManager: SpecificationManager,
																	repositoryUtils: RepositoryUtils,
																	reportManager: ReportManager,
																	communityManager: CommunityManager,
																	communityLabelManager: CommunityLabelManager,
																	testSuiteManager: TestSuiteManager,
																	testCaseManager: TestCaseManager,
																	authorizationManager: AuthorizationManager)
																 (implicit ec: ExecutionContext) extends AbstractController(cc) {

	def updateTestSuiteMetadata(testSuiteId:Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canEditTestSuite(request, testSuiteId).flatMap { _ =>
			val name:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
			val version:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.VERSION)
			val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESCRIPTION)
			val specReference = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SPEC_REFERENCE)
			val specDescription = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SPEC_DESCRIPTION)
			val specLink = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SPEC_LINK)
			var documentation:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DOCUMENTATION)
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
			val name:String = ParameterExtractor.requiredBodyParameter(request, ParameterNames.NAME)
			val description:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DESCRIPTION)
			var documentation:Option[String] = ParameterExtractor.optionalBodyParameter(request, ParameterNames.DOCUMENTATION)
			val isOptional = ParameterExtractor.optionalBooleanBodyParameter(request, ParameterNames.OPTIONAL).getOrElse(false)
			val isDisabled = ParameterExtractor.optionalBooleanBodyParameter(request, ParameterNames.DISABLED).getOrElse(false)
			val tags = sanitizeTags(ParameterExtractor.optionalBodyParameter(request, ParameterNames.TAGS))
			val specReference = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SPEC_REFERENCE)
			val specDescription = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SPEC_DESCRIPTION)
			val specLink = ParameterExtractor.optionalBodyParameter(request, ParameterNames.SPEC_LINK)
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
			val documentation = HtmlUtil.sanitizeEditorContent(ParameterExtractor.requiredBodyParameter(request, ParameterNames.DOCUMENTATION))
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
			val domainIds = ParameterExtractor.extractLongIdsBodyParameter(request, ParameterNames.DOMAIN_IDS)
			val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, ParameterNames.SPEC_IDS)
			val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, ParameterNames.GROUP_IDS)
			val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, ParameterNames.ACTOR_IDS)
			testSuiteManager.searchTestSuites(domainIds, specificationIds, groupIds, actorIds).map { results =>
				val json = JsonUtil.jsTestSuiteList(results).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
		}
	}

	def searchTestSuitesInDomain(): Action[AnyContent] = authorizedAction.async { request =>
		val domainId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.DOMAIN_ID).toLong
		authorizationManager.canViewDomains(request, Some(List(domainId))).flatMap { _ =>
			val specificationIds = ParameterExtractor.extractLongIdsBodyParameter(request, ParameterNames.SPEC_IDS)
			val groupIds = ParameterExtractor.extractLongIdsBodyParameter(request, ParameterNames.GROUP_IDS)
			val actorIds = ParameterExtractor.extractLongIdsBodyParameter(request, ParameterNames.ACTOR_IDS)
			testSuiteManager.searchTestSuites(Some(List(domainId)), specificationIds, groupIds, actorIds).map { results =>
				val json = JsonUtil.jsTestSuiteList(results).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
		}
	}

	def getTestSuiteTestCasesWithPaging(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canViewTestSuite(request, testSuiteId).flatMap { _ =>
			val filter = ParameterExtractor.optionalQueryParameter(request, ParameterNames.FILTER)
			val page = ParameterExtractor.extractPageNumber(request)
			val limit = ParameterExtractor.extractPageLimit(request)
			testSuiteManager.getTestSuiteTestCasesWithPaging(testSuiteId, filter, page, limit).map { result =>
				val json: String = JsonUtil.jsSearchResult(result, JsonUtil.jsTestSuiteTestCases).toString
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

	def moveTestSuiteToSpecification(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		val specificationId = ParameterExtractor.requiredBodyParameter(request, ParameterNames.SPECIFICATION_ID).toLong
		authorizationManager.canMoveTestSuite(request, testSuiteId, specificationId).flatMap { _ =>
			testSuiteManager.moveTestSuiteToSpecification(testSuiteId, specificationId).map { _ =>
				ResponseConstructor.constructEmptyResponse
			}.recoverWith {
				case e: UserException =>
					e.errorCode match {
						case ErrorCodes.TEST_SUITE_EXISTS => communityLabelManager.getLabel(request, LabelType.Specification, single = true, lowercase = true).map { specLabel =>
							ResponseConstructor.constructErrorResponse(e.errorCode, "A suite with the same identifier already exists in the target %s.".formatted(specLabel))
						}
						case ErrorCodes.SHARED_TEST_SUITE_EXISTS => communityLabelManager.getLabel(request, LabelType.Specification, single = true, lowercase = true).map { specLabel =>
							ResponseConstructor.constructErrorResponse(e.errorCode, "A shared suite with the same identifier already exists in the target %s.".formatted(specLabel))
						}
						case _ => Future.successful {
							ResponseConstructor.constructErrorResponse(e.errorCode, e.message)
						}
					}
			}
		}
	}

	def convertNonSharedTestSuiteToShared(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canManageTestSuite(request, testSuiteId).flatMap { _ =>
			testSuiteManager.convertNonSharedTestSuiteToShared(testSuiteId).map { _ =>
				ResponseConstructor.constructEmptyResponse
			}.recoverWith {
				case e: UserException =>
					e.errorCode match {
						case ErrorCodes.SHARED_TEST_SUITE_EXISTS => communityLabelManager.getLabel(request, LabelType.Domain, single = true, lowercase = true).map { domainLabel =>
							ResponseConstructor.constructErrorResponse(e.errorCode, "A shared suite with the same identifier already exists in the target %s.".formatted(domainLabel))
						}
						case _ => Future.successful {
							ResponseConstructor.constructErrorResponse(e.errorCode, e.message)
						}
					}
			}
		}
	}

	def convertSharedTestSuiteToNonShared(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canManageTestSuite(request, testSuiteId).flatMap { _ =>
			testSuiteManager.convertSharedTestSuiteToNonShared(testSuiteId).map { linkedSpecification =>
				ResponseConstructor.constructJsonResponse(JsonUtil.jsId(linkedSpecification).toString())
			}.recoverWith {
				case e: UserException =>
					e.errorCode match {
						case ErrorCodes.SHARED_TEST_SUITE_LINKED_TO_NO_SPECIFICATIONS => communityLabelManager.getLabel(request, LabelType.Specification, single = true, lowercase = true).map { specLabel =>
							ResponseConstructor.constructErrorResponse(e.errorCode, "The test suite to convert must be linked to the target %s.".formatted(specLabel))
						}
						case ErrorCodes.SHARED_TEST_SUITE_LINKED_TO_MULTIPLE_SPECIFICATIONS => communityLabelManager.getLabel(request, LabelType.Specification, single = true, lowercase = true).map { specLabel =>
							ResponseConstructor.constructErrorResponse(e.errorCode, "The test suite to convert must be linked to a single target %s.".formatted(specLabel))
						}
						case _ => Future.successful {
							ResponseConstructor.constructErrorResponse(e.errorCode, e.message)
						}
					}
			}
		}
	}

	def getAvailableSpecificationsForMove(testSuiteId: Long): Action[AnyContent] = authorizedAction.async { request =>
		authorizationManager.canManageTestSuite(request, testSuiteId).flatMap { _ =>
			specificationManager.getSpecificationsForTestSuiteMove(testSuiteId).map { specs =>
				val json = JsonUtil.jsSpecifications(specs).toString()
				ResponseConstructor.constructJsonResponse(json)
			}
		}
	}

}