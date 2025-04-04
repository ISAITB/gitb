package controllers

import actors.events.TestSessionStartedEvent
import actors.events.sessions.TerminateAllSessionsEvent
import com.gitb.tbs._
import config.Configurations
import controllers.util._
import exceptions.ErrorCodes
import managers._
import managers.triggers.TriggerHelper
import models.SessionConfigurationData
import org.apache.commons.io.FileUtils
import org.apache.pekko.actor.ActorSystem
import play.api.mvc._
import utils._

import java.nio.file.Path
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestService @Inject() (authorizedAction: AuthorizedAction,
                             cc: ControllerComponents,
                             actorManager: ActorManager,
                             authorizationManager: AuthorizationManager,
                             testbedClient: managers.TestbedBackendClient,
                             actorSystem: ActorSystem,
                             testResultManager: TestResultManager,
                             testExecutionManager: TestExecutionManager,
                             triggerHelper: TriggerHelper)
                            (implicit ec: ExecutionContext) extends AbstractController(cc) {

  private def getTestCasePresentationByDomain(testId:String, domainId: Long): Future[GetTestCaseDefinitionResponse] = {
    getSessionConfigurationData(domainId).flatMap { configData =>
      testbedClient.getTestCaseDefinition(testId, None, configData)
    }
  }

  def getTestCasePresentationByStatement(testId:String, sessionId: Option[String], actorId: Long, systemId: Long): Future[GetTestCaseDefinitionResponse] = {
    getSessionConfigurationData(systemId, actorId, onlySimple = true).flatMap { configData =>
      testbedClient.getTestCaseDefinition(testId, sessionId, configData)
    }
  }

  /**
   * Gets the test case definition for a specific test
   */
  def getTestCaseDefinitionByStatement(test_id:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestCase(request, test_id).flatMap { _ =>
      val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR).toLong
      val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM).toLong
      getTestCasePresentationByStatement(test_id, None, actorId, systemId).map { response =>
        val json = JacksonUtil.serializeTestCasePresentation(response.getTestcase)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the test case definition for a specific test
   */
  def getTestCaseDefinitionByDomain(test_id:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewTestCase(request, test_id).flatMap { _ =>
      val domainId = ParameterExtractor.requiredQueryParameter(request, Parameters.DOMAIN).toLong
      getTestCasePresentationByDomain(test_id, domainId).map { response =>
        val json = JacksonUtil.serializeTestCasePresentation(response.getTestcase)
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Gets the definition for a actor test
   */
  def getActorDefinitions: Action[AnyContent] = authorizedAction.async { request =>
    val specId = ParameterExtractor.requiredQueryParameter(request, Parameters.SPECIFICATION_ID).toLong
    authorizationManager.canViewActorsBySpecificationId(request, specId).flatMap { _ =>
      actorManager.getActorsWithSpecificationId(None, Some(List(specId))).map { actors =>
        val json = JsonUtil.jsActorsNonCase(actors).toString()
        ResponseConstructor.constructJsonResponse(json)
      }
    }
  }

  /**
   * Initiates the test case
   */
  def initiate(test_id:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestCase(request, test_id).flatMap { _ =>
      testbedClient.initiate(test_id.toLong, None).map { response =>
        ResponseConstructor.constructStringResponse(response)
      }
    }
  }

  private def getSessionConfigurationData(domainId: Long): Future[SessionConfigurationData] = {
    testExecutionManager.loadDomainParametersByDomainId(domainId, onlySimple = true).map { domainParameters =>
      SessionConfigurationData(
        None,
        domainParameters,
        None,
        None
      )
    }
  }

  private def getSessionConfigurationData(systemId: Long, actorId: Long, onlySimple: Boolean): Future[SessionConfigurationData] = {
    testExecutionManager.loadConformanceStatementParameters(systemId, actorId, onlySimple).zip(
      testExecutionManager.loadDomainParametersByActorId(actorId, onlySimple).zip(
        testExecutionManager.loadOrganisationParameters(systemId, onlySimple).zip(
          testExecutionManager.loadSystemParameters(systemId, onlySimple)
        )
      )
    ).map { x =>
      SessionConfigurationData(
        Some(x._1),
        x._2._1,
        Some(x._2._2._1._2),
        Some(x._2._2._2)
      )
    }
  }

  /**
   * Sends the required data on preliminary steps
   */
  def configure(sessionId:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, sessionId).flatMap { _ =>
      val systemId = ParameterExtractor.requiredQueryParameter(request, Parameters.SYSTEM_ID).toLong
      val actorId = ParameterExtractor.requiredQueryParameter(request, Parameters.ACTOR_ID).toLong
      getSessionConfigurationData(systemId, actorId, onlySimple = false).flatMap { config =>
        testbedClient.configure(sessionId, config, None).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  private def provideInputInternal(sessionId: String, request: RequestWithAttributes[AnyContent], isAdmin: Boolean): Future[Result] = {
    val paramMap = ParameterExtractor.paramMap(request)
    val files = ParameterExtractor.extractFiles(request)
    var response: Result = null
    // Check for viruses in the uploaded file(s)
    if (Configurations.ANTIVIRUS_SERVER_ENABLED && ParameterExtractor.virusPresentInFiles(files.map(_._2.file))) {
      response = ResponseConstructor.constructBadRequestResponse(ErrorCodes.VIRUS_FOUND, "Provided file failed virus scan.")
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
          userInput.setFileName(Path.of(fileInfo.name).getFileName.toString)
        }
      }
      testbedClient.provideInput(sessionId, step, Some(userInputs), isAdmin).flatMap { _ =>
        // Delete the test interaction entry.
        testResultManager.deleteTestInteractionsWrapper(sessionId, Some(step)).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    } else {
      Future.successful(response)
    }
  }

  /**
   * Sends inputs to the TestbedService
   */
  def provideInput(sessionId:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, sessionId).flatMap { _ =>
      provideInputInternal(sessionId, request, isAdmin = false)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Sends inputs to the TestbedService as an administrator
   */
  def provideInputAdmin(sessionId:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, sessionId, requireAdmin = true).flatMap { _ =>
      provideInputInternal(sessionId, request, isAdmin = true)
    }.andThen { _ =>
      if (request.body.asMultipartFormData.isDefined) request.body.asMultipartFormData.get.files.foreach { file => FileUtils.deleteQuietly(file.ref) }
    }
  }

  /**
   * Starts the preliminary phase if test case description has one
   */
  def initiatePreliminary(session_id:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, session_id).flatMap { _ =>
      testbedClient.initiatePreliminary(session_id).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  private def callSessionStartTrigger(sessionId: String): Future[Unit] = {
    testResultManager.getCommunityIdForTestSession(sessionId).map { ids =>
      if (ids.isDefined && ids.get._2.isDefined) {
        triggerHelper.publishTriggerEvent(new TestSessionStartedEvent(ids.get._2.get, ids.get._1))
      }
    }
  }

  /**
   * Starts the test case
   */
  def start(sessionId:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, sessionId).flatMap { _ =>
      callSessionStartTrigger(sessionId).flatMap { _ =>
        testbedClient.start(sessionId).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  /**
   * Stops the test case
   */
  def stop(session_id:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, session_id).flatMap { _ =>
      testExecutionManager.endSession(session_id).map { _ =>
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

  def stopAll(): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageAnyTestSession(request).flatMap { _ =>
      actorSystem.eventStream.publish(TerminateAllSessionsEvent(None, None, None))
      testResultManager.getAllRunningSessions().flatMap { sessions =>
        val stopTasks = sessions.map {sessionId =>
          testExecutionManager.endSession(sessionId)
        }
        // Wait for all stop Futures to complete
        Future.sequence(stopTasks).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def stopAllCommunitySessions(communityId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canManageCommunity(request, communityId).flatMap { _ =>
      actorSystem.eventStream.publish(TerminateAllSessionsEvent(Some(communityId), None, None))
      testResultManager.getRunningSessionsForCommunity(communityId).flatMap { sessions =>
        val stopTasks = sessions.map { sessionId =>
          testExecutionManager.endSession(sessionId)
        }
        Future.sequence(stopTasks).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def stopAllOrganisationSessions(organisationId: Long): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canViewOrganisation(request, organisationId).flatMap { _ =>
      actorSystem.eventStream.publish(TerminateAllSessionsEvent(None, Some(organisationId), None))
      testResultManager.getRunningSessionsForOrganisation(organisationId).flatMap { sessions =>
        val stopTasks = sessions.map { sessionId =>
          testExecutionManager.endSession(sessionId)
        }
        Future.sequence(stopTasks).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  /**
   * Restarts the test case with same preliminary data
   */
  def restart(session_id:String): Action[AnyContent] = authorizedAction.async { request =>
    authorizationManager.canExecuteTestSession(request, session_id).flatMap { _ =>
      callSessionStartTrigger(session_id).flatMap { _ =>
        testbedClient.restart(session_id).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    }
  }

  def startHeadlessTestSessions(): Action[AnyContent] = authorizedAction.async { request =>
    val testCaseIds = ParameterExtractor.optionalLongListBodyParameter(request, Parameters.TEST_CASE_IDS)
    val specId = ParameterExtractor.requiredBodyParameter(request, Parameters.SPECIFICATION_ID).toLong
    val systemId = ParameterExtractor.requiredBodyParameter(request, Parameters.SYSTEM_ID).toLong
    val actorId = ParameterExtractor.requiredBodyParameter(request, Parameters.ACTOR_ID).toLong
    val forceSequentialExecution = ParameterExtractor.optionalBodyParameter(request, Parameters.SEQUENTIAL).getOrElse("false").toBoolean
    if (testCaseIds.isDefined && testCaseIds.get.nonEmpty) {
      authorizationManager.canExecuteTestCases(request, testCaseIds.get, specId, systemId, actorId).flatMap { _ =>
        testExecutionManager.startHeadlessTestSessions(testCaseIds.get, systemId, actorId, None, None, forceSequentialExecution).map { _ =>
          ResponseConstructor.constructEmptyResponse
        }
      }
    } else {
      Future.successful {
        ResponseConstructor.constructEmptyResponse
      }
    }
  }

}
