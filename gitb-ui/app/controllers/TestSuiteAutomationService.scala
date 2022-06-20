package controllers

import config.Configurations
import controllers.util.{AuthorizedAction, ResponseConstructor}
import exceptions.ErrorCodes
import managers.{AuthorizationManager, ConformanceManager, SpecificationManager, TestSuiteManager}
import models.Constants
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.RandomStringUtils
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import utils.{ClamAVClient, JsonUtil, RepositoryUtils}

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Paths}
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.util.Using

@Singleton
class TestSuiteAutomationService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, authorizationManager: AuthorizationManager, repositoryUtils: RepositoryUtils, specificationManager: SpecificationManager, testSuiteManager: TestSuiteManager, conformanceManager: ConformanceManager) extends BaseAutomationService(cc) {

  def deploy: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, authorizationManager.canManageTestSuitesThroughAutomationApi,
      { body =>
        var response: Result = null
        val communityKey = request.headers.get(Constants.AutomationHeader).get
        val input = JsonUtil.parseJsTestSuiteDeployRequest(body)
        val specification = specificationManager.getSpecificationByApiKeys(input.specification, communityKey)
        if (specification.isDefined) {
          val testSuiteBytes = Base64.getDecoder.decode(input.testSuite)
          if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
            val virusScanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
            Using(new ByteArrayInputStream(testSuiteBytes)) { stream =>
              val scanResult = virusScanner.scan(stream)
              if (!ClamAVClient.isCleanReply(scanResult)) {
                response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Test suite failed virus scan.")
              }
            }
          }
          if (response == null) {
            val testSuiteFileName = "ts_"+RandomStringUtils.random(10, false, true)+".zip"
            val testSuiteFile = Paths.get(
              repositoryUtils.getTempFolder().getAbsolutePath,
              RandomStringUtils.random(10, false, true),
              testSuiteFileName
            )
            testSuiteFile.toFile.getParentFile.mkdirs()
            try {
              Files.write(testSuiteFile, testSuiteBytes)
              val result = testSuiteManager.deployTestSuiteFromApi(specification.get, input, testSuiteFile.toFile)
              response = ResponseConstructor.constructJsonResponse(JsonUtil.jsTestSuiteDeployInfo(result).toString)
            } finally {
              FileUtils.deleteQuietly(testSuiteFile.toFile)
            }
          }
        } else {
          response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.INVALID_REQUEST, "Specification could not be determined from provided API keys")
        }
        response
      }
    )
  }

  def undeploy: Action[AnyContent] = authorizedAction { request =>
    processAsJson(request, authorizationManager.canManageTestSuitesThroughAutomationApi,
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
