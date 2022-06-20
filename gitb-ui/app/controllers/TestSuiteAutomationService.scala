package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ParameterExtractor, ResponseConstructor}
import exceptions.{ErrorCodes, InvalidRequestException}
import managers.{AuthorizationManager, ConformanceManager, SpecificationManager, TestSuiteManager}
import models.Constants
import models.automation.TestSuiteDeployRequest
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import play.api.mvc._
import utils.{ClamAVClient, JsonUtil, RepositoryUtils}

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.util.Using

@Singleton
class TestSuiteAutomationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, authorizationManager: AuthorizationManager, repositoryUtils: RepositoryUtils, specificationManager: SpecificationManager, testSuiteManager: TestSuiteManager, conformanceManager: ConformanceManager) extends BaseAutomationService(cc) {

  private def deployInternal(input: TestSuiteDeployRequest, testSuiteArchive: File, request: Request[AnyContent]) = {
    var response: Result = null
    val communityKey = request.headers.get(Constants.AutomationHeader).get
    val specification = specificationManager.getSpecificationByApiKeys(input.specification, communityKey)
    if (specification.isDefined) {
      if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
        val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
        Using(Files.newInputStream(testSuiteArchive.toPath)) { stream =>
          val scanResult = virusScanner.scan(stream)
          if (!ClamAVClient.isCleanReply(scanResult)) {
            response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Test suite failed virus scan.")
          }
        }
      }
      if (response == null) {
          val result = testSuiteManager.deployTestSuiteFromApi(specification.get, input, testSuiteArchive)
          response = ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSuiteDeployInfo(result).toString)
      }
    } else {
      response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Specification could not be determined from provided API keys")
    }
    response
  }

  def deploy: Action[AnyContent] = authorizedAction { request =>
    var testSuiteArchive: File = null
    try {
      authorizationManager.canManageTestSuitesThroughAutomationApi(request)
      var response: Result = null
      if (request.body.asMultipartFormData.isDefined) {
        // Multipart form request.
        val params = Some(request.body.asMultipartFormData.get.dataParts)
        val input = TestSuiteDeployRequest(
          ParameterExtractor.requiredBodyParameter(params, "specification"),
          ParameterExtractor.optionalBodyParameter(params, "ignoreWarnings").getOrElse("false").toBoolean,
          ParameterExtractor.optionalBodyParameter(params, "replaceTestHistory").getOrElse("false").toBoolean,
          ParameterExtractor.optionalBodyParameter(params, "updateSpecification").getOrElse("false").toBoolean
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
          val input = JsonUtil.parseJsTestSuiteDeployRequest(body)
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
        ResponseConstructor.constructBadRequestResponse(e.getError, e.getMessage)
      case e: Exception =>
        ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, e.getMessage)
    } finally {
      FileUtils.deleteQuietly(testSuiteArchive)
    }
  }

  def undeploy: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, Some(authorizationManager.canManageTestSuitesThroughAutomationApi),
      { body =>
        val communityKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSuiteUndeployRequest(body)
        val testSuiteId = testSuiteManager.getTestSuiteIdByApiKeys(communityKey, input.specification, input.testSuite)
        if (testSuiteId.isDefined) {
          conformanceManager.undeployTestSuiteWrapper(testSuiteId.get)
          ResponseConstructor.constructEmptyResponse
        } else {
          ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Test suite could not be determined from provided API keys")
        }
      })
  }

}
