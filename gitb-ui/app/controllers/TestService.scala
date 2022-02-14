package controllers

import actors.events.sessions.TerminateAllSessionsEvent
import akka.actor.ActorSystem
import com.gitb.tbs._
import config.Configurations
import controllers.util._
import exceptions.ErrorCodes
import managers._
import org.apache.commons.io.FileUtils
import play.api.mvc._
import utils._

import javax.inject.{Inject, Singleton}

@Singleton
class TestService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, conformanceManager: ConformanceManager, authorizationManager: AuthorizationManager, testbedClient: managers.TestbedBackendClient, actorSystem: ActorSystem, testResultManager: TestResultManager, testExecutionManager: TestExecutionManager) extends AbstractController(cc) {

  def getTestCasePresentation(testId:String, sessionId: Option[String]): GetTestCaseDefinitionResponse = {
    testbedClient.getTestCaseDefinition(testId, sessionId)
  }

  /**
   * Gets the test case definition for a specific test
   */
  def getTestCaseDefinition(test_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canViewTestCase(request, test_id)
    val response = getTestCasePresentation(test_id, None)
    val json = JacksonUtil.serializeTestCasePresentation(response.getTestcase)
    ResponseConstructor.constructJsonResponse(json)
  }
  /**
   * Gets the definition for a actor test
   */
  def getActorDefinitions: Action[AnyContent] = authorizedAction { request =>
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    authorizationManager.canViewActorsBySpecificationId(request, specId)
    val actors = conformanceManager.getActorsWithSpecificationId(None, Some(specId))
    val json = JsonUtil.jsActorsNonCase(actors).toString()
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Initiates the test case
   */
  def initiate(test_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestCase(request, test_id)
    ResponseConstructor.constructStringResponse(testbedClient.initiate(test_id.toLong, None))
  }

  /**
   * Sends the required data on preliminary steps
   */
  def configure(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, sessionId)
    val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
    val organisationData = testExecutionManager.loadOrganisationParameters(systemId)
    val response = testbedClient.configure(
      sessionId,
      testExecutionManager.loadConformanceStatementParameters(systemId, actorId),
      testExecutionManager.loadDomainParameters(actorId),
      organisationData._2,
      testExecutionManager.loadSystemParameters(systemId),
      None
    )
    val json = JacksonUtil.serializeConfigureResponse(response)
    ResponseConstructor.constructJsonResponse(json)
  }

  /**
   * Sends inputs to the TestbedService
   */
  def provideInput(session_id:String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canExecuteTestSession(request, session_id)
      val paramMap = ParameterExtractor.paramMap(request)
      val files = ParameterExtractor.extractFiles(request)
      var response: Result = null
      if (Configurations.ANTIVIRUS_SERVER_ENABLED) {
        // Check for viruses in the uploaded file(s)
        val scanner = new ClamAVClient(Configurations.ANTIVIRUS_SERVER_HOST, Configurations.ANTIVIRUS_SERVER_PORT, Configurations.ANTIVIRUS_SERVER_TIMEOUT)
        files.foreach { file =>
          if (response == null) {
            val scanResult = scanner.scan(file._2.file)
            if (!ClamAVClient.isCleanReply(scanResult)) {
              response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Provided file failed virus scan.")
            }
          }
        }
      }
      if (response == null) {
        val inputs = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.INPUTS)
        val step   = ParameterExtractor.requiredBodyParameter(paramMap, Parameters.TEST_STEP)
        val userInputs = JsonUtil.parseJsUserInputs(inputs)
        // Set files to inputs.
        userInputs.foreach { userInput =>
          if (userInput.getValue == null && files.contains(s"file_${userInput.getId}")) {
            val fileInfo = files(s"file_${userInput.getId}")
            userInput.setValue(MimeUtil.getFileAsDataURL(fileInfo.file, fileInfo.contentType.orNull))
          }
        }
        testbedClient.provideInput(session_id, step, Some(userInputs))
        response = ResponseConstructor.constructEmptyResponse
      }
      response
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Starts the preliminary phase if test case description has one
   */
  def initiatePreliminary(session_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)
    testbedClient.initiatePreliminary(session_id)
    ResponseConstructor.constructEmptyResponse

  }
  /**
   * Starts the test case
   */
  def start(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, sessionId)
    testbedClient.start(sessionId)
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Stops the test case
   */
  def stop(session_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)
    testExecutionManager.endSession(session_id)
    ResponseConstructor.constructEmptyResponse
  }

  def stopAll(): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageAnyTestSession(request)
    actorSystem.eventStream.publish(TerminateAllSessionsEvent(None, None, None))
    testResultManager.getAllRunningSessions().foreach { sessionId =>
      testExecutionManager.endSession(sessionId)
    }
    ResponseConstructor.constructEmptyResponse
  }

  def stopAllCommunitySessions(communityId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageCommunity(request, communityId)
    actorSystem.eventStream.publish(TerminateAllSessionsEvent(Some(communityId), None, None))
    testResultManager.getRunningSessionsForCommunity(communityId).foreach { sessionId =>
      testExecutionManager.endSession(sessionId)
    }
    ResponseConstructor.constructEmptyResponse
  }

  def stopAllOrganisationSessions(organisationId: Long): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canManageOrganisationBasic(request, organisationId)
    actorSystem.eventStream.publish(TerminateAllSessionsEvent(None, Some(organisationId), None))
    testResultManager.getRunningSessionsForOrganisation(organisationId).foreach { sessionId =>
      testExecutionManager.endSession(sessionId)
    }
    ResponseConstructor.constructEmptyResponse
  }

  /**
   * Restarts the test case with same preliminary data
   */
  def restart(session_id:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, session_id)
    testbedClient.restart(session_id)
    ResponseConstructor.constructEmptyResponse
  }

  def startHeadlessTestSessions(): Action[AnyContent] = authorizedAction { request =>
    val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
    val specId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val forceSequentialExecution = ParameterExtractor.optionalBodyParameter(request, Parameters.SEQUENTIAL).getOrElse("false").toBoolean
    if (testCaseIds.isDefined && testCaseIds.get.nonEmpty) {
      authorizationManager.canExecuteTestCases(request, testCaseIds.get, specId, systemId, actorId)
      testExecutionManager.startHeadlessTestSessions(testCaseIds.get, systemId, actorId, None, None, forceSequentialExecution)
    }
    ResponseConstructor.constructEmptyResponse
  }

}
