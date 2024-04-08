package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, RequestWithAttributes, ResponseConstructor}
import exceptions.{ErrorCodes, InvalidRequestException}
import managers.{AuthorizationManager, SpecificationManager, TestSuiteManager}
import models.{Constants, TestCaseDeploymentAction}
import models.automation.TestSuiteDeployRequest
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.{RandomStringUtils, StringUtils}
import org.slf4j.LoggerFactory
import play.api.mvc._
import utils.{ClamAVClient, JsonUtil, RepositoryUtils}

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.util.Using

@Singleton
class TestSuiteAutomationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, authorizationManager: AuthorizationManager, repositoryUtils: RepositoryUtils, specificationManager: SpecificationManager, testSuiteManager: TestSuiteManager) extends BaseAutomationService(cc) {

  private val LOG = LoggerFactory.getLogger(this.getClass)

  private def deployInternal(input: TestSuiteDeployRequest, testSuiteArchive: File, request: Request[AnyContent]) = {
    var response: Result = null
    val communityKey = request.headers.get(Constants.AutomationHeader).get

    val ids = specificationManager.getSpecificationInfoByApiKeys(input.specification, communityKey)
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
        val result = testSuiteManager.deployTestSuiteFromApi(ids.get._1, ids.get._2, input, testSuiteArchive)
        response = ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSuiteDeployInfo(result).toString)
      }
    } else {
      response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Specification could not be determined from provided API keys.")
    }
    response
  }

  private def handleDeploy(request: RequestWithAttributes[AnyContent], sharedTestSuite: Boolean) = {
    var testSuiteArchive: File = null
    try {
      authorizationManager.canManageTestSuitesThroughAutomationApi(request)
      var response: Result = null
      if (request.body.asMultipartFormData.isDefined) {
        // Multipart form request.
        val params = Some(request.body.asMultipartFormData.get.dataParts)
        val defaultReplaceTestHistory = ParameterExtractor.optionalBodyParameter(params, "replaceTestHistory").map(_.toBoolean)
        val defaultUpdateSpecification = ParameterExtractor.optionalBodyParameter(params, "updateSpecification").map(_.toBoolean)
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
          sharedTestSuite
        )
        val uploadedFile = request.body.asMultipartFormData.get.file("testSuite")
        if (uploadedFile.isDefined) {
          testSuiteArchive = uploadedFile.get.ref
          response = deployInternal(input, testSuiteArchive, request)
        } else {
          response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite archive was not provided. Expected a file part named 'testSuite'.")
        }
      } else {
        // JSON request.
        response = processAsJson(request, None, { body =>
          val input = JsonUtil.parseJsTestSuiteDeployRequest(body, sharedTestSuite)
          val testSuiteFileName = "ts_" + RandomStringUtils.random(10, false, true) + ".zip"
          val testSuiteFile = Paths.get(
            repositoryUtils.getTempFolder().getAbsolutePath,
            RandomStringUtils.random(10, false, true),
            testSuiteFileName
          )
          testSuiteFile.toFile.getParentFile.mkdirs()
          Files.write(testSuiteFile, Base64.getDecoder.decode(input._2))
          deployInternal(input._1, testSuiteFile.toFile, request)
        })
      }
      response
    } catch {
      case e: InvalidRequestException =>
        LOG.warn("Caught invalid request error from test suite API")
        ResponseConstructor.constructBadRequestResponse(e.getError, e.getMessage)
      case e: Exception =>
        LOG.warn("Caught general error from test suite API", e)
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, e.getMessage)
    } finally {
      FileUtils.deleteQuietly(testSuiteArchive)
    }
  }

  def deploy: Action[AnyContent] = authorizedAction { request =>
    handleDeploy(request, sharedTestSuite = false)
  }

  def deployShared: Action[AnyContent] = authorizedAction { request =>
    handleDeploy(request, sharedTestSuite = true)
  }

  private def handleUndeploy(request: RequestWithAttributes[AnyContent], sharedTestSuite: Boolean) = {
    processAsJson(request, Some(authorizationManager.canManageTestSuitesThroughAutomationApi),
      { body =>
        val communityKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSuiteUndeployRequest(body, sharedTestSuite)
        val testSuiteInfo = testSuiteManager.getTestSuiteInfoByApiKeys(communityKey, input.specification, input.testSuite)
        if (testSuiteInfo.isDefined) {
          if (sharedTestSuite && !testSuiteInfo.get._3) {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "The identified test suite is not defined as being shared. Removal was skipped.")
          } else if (!sharedTestSuite && testSuiteInfo.get._3) {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "The identified test suite is defined as being shared. Removal was skipped.")
          } else {
            testSuiteManager.undeployTestSuiteWrapper(testSuiteInfo.get._1)
            ResponseConstructor.constructEmptyResponse
          }
        } else {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite could not be determined from the provided API keys.")
        }
      }
    )
  }

  def undeploy: Action[AnyContent] = authorizedAction { request =>
    handleUndeploy(request, sharedTestSuite = false)
  }

  def undeployShared: Action[AnyContent] = authorizedAction { request =>
    handleUndeploy(request, sharedTestSuite = true)
  }

  def linkShared: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageTestSuitesThroughAutomationApi),
      { body =>
        val communityKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSuiteLinkRequest(body)
        val testSuiteInfo = testSuiteManager.getTestSuiteInfoByApiKeys(communityKey, None, input.testSuite)
        if (testSuiteInfo.isDefined) {
          if (input.specifications.nonEmpty) {
            val result = testSuiteManager.linkSharedTestSuiteFromApi(testSuiteInfo.get._1, testSuiteInfo.get._2, input)
            ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSuiteLinkResponseSpecifications(result).toString)
          } else {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "At least one target specification must be provided.")
          }
        } else {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite could not be determined from the provided API keys.")
        }
      }
    )
  }

  def unlinkShared: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageTestSuitesThroughAutomationApi),
      { body =>
        val communityKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSuiteUnlinkRequest(body)
        val testSuiteInfo = testSuiteManager.getTestSuiteInfoByApiKeys(communityKey, None, input.testSuite)
        if (testSuiteInfo.isDefined) {
          if (input.specifications.nonEmpty) {
            testSuiteManager.unlinkSharedTestSuiteFromApi(testSuiteInfo.get._1, testSuiteInfo.get._2, input)
            ResponseConstructor.constructEmptyResponse
          } else {
            ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "At least one target specification must be provided.")
          }
        } else {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite could not be determined from the provided API keys.")
        }
      }
    )
  }
}
