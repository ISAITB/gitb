package controllers.rest

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, RequestWithAttributes, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthorizationManager, SpecificationManager, TestSuiteManager}
import models.automation.TestSuiteDeployRequest
import models.{Constants, TestCaseDeploymentAction}
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import play.api.mvc._
import utils.{ClamAVClient, JsonUtil, RepositoryUtils}

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

@Singleton
class TestSuiteAutomationService @Inject() (authorizedAction: AuthorizedAction,
                                            cc: ControllerComponents,
                                            authorizationManager: AuthorizationManager,
                                            repositoryUtils: RepositoryUtils,
                                            specificationManager: SpecificationManager,
                                            testSuiteManager: TestSuiteManager)
                                           (implicit ec: ExecutionContext) extends BaseAutomationService(cc) {

  private def deployInternal(input: TestSuiteDeployRequest, testSuiteArchive: File, request: Request[AnyContent]): Future[Result] = {
    val communityKey = request.headers.get(Constants.AutomationHeader).get
    specificationManager.getSpecificationInfoByApiKeys(input.specification, communityKey).flatMap { ids =>
      var response: Result = null
      if (input.specification.isEmpty && !input.sharedTestSuite) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "A specification API key was expected.")
      } else if (input.specification.isDefined && (ids.isEmpty || ids.get._2.isEmpty)) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "No specification could be identified based on the provided API keys.")
      } else if (input.specification.isEmpty && ids.isEmpty) {
        response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "No domain could be identified based on the provided API key.")
      }
      if (response == null) {
        if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
          val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
          Using.resource(Files.newInputStream(testSuiteArchive.toPath)) { stream =>
            val scanResult = virusScanner.scan(stream)
            if (!ClamAVClient.isCleanReply(scanResult)) {
              response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Test suite failed virus scan.")
            }
          }
        }
        if (response == null) {
          testSuiteManager.deployTestSuiteFromApi(ids.get._1, ids.get._2, input, testSuiteArchive).map { result =>
            ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSuiteDeployInfo(result, input.showIdentifiers).toString)
          }
        } else {
          Future.successful(response)
        }
      } else {
        Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Specification could not be determined from provided API keys."))
      }
    }
  }

  private def handleDeploy(request: RequestWithAttributes[AnyContent], sharedTestSuite: Boolean): Future[Result] = {
    (for {
      _ <- authorizationManager.canManageTestSuitesThroughAutomationApi(request)
      result <- {
        if (request.body.asMultipartFormData.isDefined) {
          // Multipart form request.
          val params = Some(request.body.asMultipartFormData.get.dataParts)
          val defaultReplaceTestHistory = ParameterExtractor.optionalBodyParameter(params, "replaceTestHistory").map(_.toBoolean)
          val defaultUpdateSpecification = ParameterExtractor.optionalBodyParameter(params, "updateSpecification").map(_.toBoolean)
          val showIdentifiers = ParameterExtractor.optionalBodyParameter(params, "showIdentifiers").forall(_.toBoolean)
          val testCaseActions = mutable.HashMap[String, TestCaseDeploymentAction]()
          // Set update specification flags.
          ParameterExtractor.optionalArrayBodyParameter(params, "testCaseWithSpecificationUpdate").getOrElse(List.empty).foreach { identifier =>
            if (StringUtils.isNotBlank(identifier)) {
              testCaseActions.put(identifier, new TestCaseDeploymentAction(identifier, Some(true), defaultReplaceTestHistory))
            }
          }
          ParameterExtractor.optionalArrayBodyParameter(params, "testCaseWithoutSpecificationUpdate").getOrElse(List.empty).foreach { identifier =>
            if (StringUtils.isNotBlank(identifier)) {
              val testCase = testCaseActions.getOrElseUpdate(identifier, new TestCaseDeploymentAction(identifier, Some(false), defaultReplaceTestHistory))
              testCase.updateDefinition = Some(false)
            }
          }
          // Set replace test history flags.
          ParameterExtractor.optionalArrayBodyParameter(params, "testCaseWithTestHistoryReplacement").getOrElse(List.empty).foreach { identifier =>
            if (StringUtils.isNotBlank(identifier)) {
              val testCase = testCaseActions.getOrElseUpdate(identifier, new TestCaseDeploymentAction(identifier, defaultUpdateSpecification, Some(true)))
              testCase.resetTestHistory = Some(true)
            }
          }
          ParameterExtractor.optionalArrayBodyParameter(params, "testCaseWithoutTestHistoryReplacement").getOrElse(List.empty).foreach { identifier =>
            if (StringUtils.isNotBlank(identifier)) {
              val testCase = testCaseActions.getOrElseUpdate(identifier, new TestCaseDeploymentAction(identifier, defaultUpdateSpecification, Some(false)))
              testCase.resetTestHistory = Some(false)
            }
          }
          val specification = if (sharedTestSuite) {
            None
          } else {
            Some(ParameterExtractor.requiredBodyParameter(params, "specification"))
          }
          val input = TestSuiteDeployRequest(
            specification,
            ParameterExtractor.optionalBodyParameter(params, "ignoreWarnings").getOrElse("false").toBoolean,
            defaultReplaceTestHistory,
            defaultUpdateSpecification,
            testCaseActions,
            sharedTestSuite,
            showIdentifiers
          )
          val uploadedFile = request.body.asMultipartFormData.get.file("testSuite")
          if (uploadedFile.isDefined) {
            deployInternal(input, uploadedFile.get.ref, request)
          } else {
            Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite archive was not provided. Expected a file part named 'testSuite'."))
          }
        } else {
          // JSON request.
          processAsJson(request, () => Future.successful(true), { body =>
            val input = JsonUtil.parseJsTestSuiteDeployRequest(body, sharedTestSuite)
            val testSuiteFileName = "ts_" + RandomStringUtils.secure.next(10, false, true) + ".zip"
            val testSuiteFile = Paths.get(
              repositoryUtils.getTempFolder().getAbsolutePath,
              RandomStringUtils.secure.next(10, false, true),
              testSuiteFileName
            )
            testSuiteFile.toFile.getParentFile.mkdirs()
            Files.write(testSuiteFile, Base64.getDecoder.decode(input._2))
            deployInternal(input._1, testSuiteFile.toFile, request)
          })
        }
      }
    } yield result).andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  def deploy: Action[AnyContent] = authorizedAction.async { request =>
    handleDeploy(request, sharedTestSuite = false)
  }

  def deployShared: Action[AnyContent] = authorizedAction.async { request =>
    handleDeploy(request, sharedTestSuite = true)
  }

  private def handleUndeploy(request: RequestWithAttributes[AnyContent], sharedTestSuite: Boolean): Future[Result] = {
    processAsJson(request, () => authorizationManager.canManageTestSuitesThroughAutomationApi(request), { body =>
        val communityKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSuiteUndeployRequest(body, sharedTestSuite)
        testSuiteManager.getTestSuiteInfoByApiKeys(communityKey, input.specification, input.testSuite).flatMap { testSuiteInfo =>
          if (testSuiteInfo.isDefined) {
            if (sharedTestSuite && !testSuiteInfo.get._3) {
              Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "The identified test suite is not defined as being shared. Removal was skipped."))
            } else if (!sharedTestSuite && testSuiteInfo.get._3) {
              Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "The identified test suite is defined as being shared. Removal was skipped."))
            } else {
              testSuiteManager.undeployTestSuiteWrapper(testSuiteInfo.get._1).flatMap { _ =>
                Future.successful(ResponseConstructor.constructEmptyResponse)
              }
            }
          } else {
            Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite could not be determined from the provided API keys."))
          }
        }
      }
    )
  }

  def undeploy: Action[AnyContent] = authorizedAction.async { request =>
    handleUndeploy(request, sharedTestSuite = false)
  }

  def undeployShared: Action[AnyContent] = authorizedAction.async { request =>
    handleUndeploy(request, sharedTestSuite = true)
  }

  def linkShared: Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageTestSuitesThroughAutomationApi(request), { body =>
        val communityKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSuiteLinkRequest(body)
        testSuiteManager.getTestSuiteInfoByApiKeys(communityKey, None, input.testSuite).flatMap { testSuiteInfo =>
          if (testSuiteInfo.isDefined) {
            if (input.specifications.nonEmpty) {
              testSuiteManager.linkSharedTestSuiteFromApi(testSuiteInfo.get._1, testSuiteInfo.get._2, input).map { result =>
                ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSuiteLinkResponseSpecifications(result).toString)
              }
            } else {
              Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "At least one target specification must be provided."))
            }
          } else {
            Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite could not be determined from the provided API keys."))
          }
        }
      }
    )
  }

  def unlinkShared: Action[AnyContent] = authorizedAction.async { request =>
    processAsJson(request, () => authorizationManager.canManageTestSuitesThroughAutomationApi(request), { body =>
        val communityKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSuiteUnlinkRequest(body)
        testSuiteManager.getTestSuiteInfoByApiKeys(communityKey, None, input.testSuite).flatMap { testSuiteInfo =>
          if (testSuiteInfo.isDefined) {
            if (input.specifications.nonEmpty) {
              testSuiteManager.unlinkSharedTestSuiteFromApi(testSuiteInfo.get._1, testSuiteInfo.get._2, input).map { _ =>
                ResponseConstructor.constructEmptyResponse
              }
            } else {
              Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "At least one target specification must be provided."))
            }
          } else {
            Future.successful(ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite could not be determined from the provided API keys."))
          }
        }
      }
    )
  }
}
