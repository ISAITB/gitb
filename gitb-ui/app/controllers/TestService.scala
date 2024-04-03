package controllers

import actors.events.TestSessionStartedEvent
import actors.events.sessions.TerminateAllSessionsEvent
import org.apache.pekko.actor.ActorSystem
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
class TestService @Inject() (authorizedAction: AuthorizedAction, cc: ControllerComponents, actorManager: ActorManager, authorizationManager: AuthorizationManager, testbedClient: managers.TestbedBackendClient, actorSystem: ActorSystem, testResultManager: TestResultManager, testExecutionManager: TestExecutionManager, triggerHelper: TriggerHelper) extends AbstractController(cc) {

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
    val actors = actorManager.getActorsWithSpecificationId(None, Some(List(specId)))
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
    testbedClient.configure(
      sessionId,
      testExecutionManager.loadConformanceStatementParameters(systemId, actorId),
      testExecutionManager.loadDomainParameters(actorId),
      organisationData._2,
      testExecutionManager.loadSystemParameters(systemId),
      None
    )
    ResponseConstructor.constructEmptyResponse
  }

  private def provideInputInternal(sessionId: String, request: RequestWithAttributes[AnyContent], isAdmin: Boolean): Result = {
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
      testbedClient.provideInput(sessionId, step, Some(userInputs), isAdmin)
      // Delete the test interaction entry.
      testResultManager.deleteTestInteractionsWrapper(sessionId, Some(step))
      response = ResponseConstructor.constructEmptyResponse
    }
    response
  }

  /**
   * Sends inputs to the TestbedService
   */
  def provideInput(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canExecuteTestSession(request, sessionId)
      provideInputInternal(sessionId, request, isAdmin = false)
    } finally {
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Sends inputs to the TestbedService as an administrator
   */
  def provideInputAdmin(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    try {
      authorizationManager.canExecuteTestSession(request, sessionId, requireAdmin = true)
      provideInputInternal(sessionId, request, isAdmin = true)
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

  private def callSessionStartTrigger(sessionId: String) = {
    val ids = testResultManager.getCommunityIdForTestSession(sessionId)
    if (ids.isDefined && ids.get._2.isDefined) {
      triggerHelper.publishTriggerEvent(new TestSessionStartedEvent(ids.get._2.get, ids.get._1))
    }
  }

  /**
   * Starts the test case
   */
  def start(sessionId:String): Action[AnyContent] = authorizedAction { request =>
    authorizationManager.canExecuteTestSession(request, sessionId)
    callSessionStartTrigger(sessionId)
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
    authorizationManager.canViewOrganisation(request, organisationId)
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
    callSessionStartTrigger(session_id)
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
