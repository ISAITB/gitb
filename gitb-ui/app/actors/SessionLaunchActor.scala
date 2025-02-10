package actors

import actors.events.TestSessionStartedEvent
import actors.events.sessions._
import com.gitb.tpl.TestCase
import managers.{ReportManager, TestbedBackendClient, TriggerHelper}
import models.{SessionConfigurationData, TypedActorConfiguration}
import org.apache.pekko.actor.{Actor, PoisonPill}
import org.slf4j.LoggerFactory

import javax.inject.Inject

object SessionLaunchActor {
  trait Factory {
    def apply(): Actor
  }
}

class SessionLaunchActor @Inject() (reportManager: ReportManager, testbedBackendClient: TestbedBackendClient, webSocketActor: WebSocketActor, triggerHelper: TriggerHelper) extends Actor {

  private val LOGGER = LoggerFactory.getLogger(classOf[SessionLaunchActor])

  override def preStart():Unit = {
    super.preStart()
    context.system.eventStream.subscribe(context.self, classOf[TestSessionConfiguredEvent])
    context.system.eventStream.subscribe(context.self, classOf[TestSessionCompletedEvent])
    context.system.eventStream.subscribe(context.self, classOf[TerminateAllSessionsEvent])
    if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor started [{}]", self.path)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(context.self)
    if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor stopped [{}]", self.path)
  }

  override def receive: Receive = active(SessionLaunchState.newState())

  def active(state: SessionLaunchState): Receive = {
    case msg: PrepareTestSessionsEvent =>
      val newState = replace(state.newForLaunchData(msg.launchData))
      if (LOGGER.isDebugEnabled()) LOGGER.debug("Preparing launch batch. {}", newState.statusText())
      self ! ProcessNextTestSessionEvent()
    case msg: TestSessionConfiguredEvent =>
      if (state.isConfiguredSession(msg.event.getTcInstanceId)) {
        sessionConfigured(msg, state)
      } else {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] received a configuration complete notification for an unknown test session [{}]", self.path, msg.event.getTcInstanceId)
      }
    case msg: TestSessionCompletedEvent =>
      if (state.isStartedSession(msg.testSession)) {
        val newState = replace(state.newForCompletedTestSession(msg.testSession))
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Completed session [{}]. {}", msg.testSession, newState.statusText())
        self ! ProcessNextTestSessionEvent()
      } else {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] received a test session complete notification for an unknown test session [{}]", self.path, msg.testSession)
      }
    case msg: TerminateSessionsEvent =>
      if (state.matchesOrganisation(msg.organisationId)) {
        val testCasesCountBefore = state.testSessionCount()
        val newState = replace(state.newWithoutTestSessions(msg.testSessions))
        val diff = testCasesCountBefore - newState.testSessionCount()
        var startNextSession = false
        if (diff > 0) {
          LOGGER.info("Cancelled {} test session(s) that were pending execution", diff)
          startNextSession = true
        }
        if (startNextSession) {
          // Checking to start another session would normally take place after we received a configuration complete event.
          self ! ProcessNextTestSessionEvent()
        }
      } else {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] received a termination request related to another organisation [{}] (expected [{}])", self.path, msg.organisationId, state.organisation())
      }
    case _: ProcessNextTestSessionEvent =>
      processNextTestSession(state)
    case msg: TerminateAllSessionsEvent =>
      if (terminationApplies(msg, state)) {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] skipping remaining test sessions", self.path)
        self ! PoisonPill.getInstance
      }
    case msg: Object =>
      LOGGER.warn("Session launch actor received unexpected message [{}]", msg.getClass.getName)
  }

  private def terminationApplies(event: TerminateAllSessionsEvent, state: SessionLaunchState): Boolean = {
    (event.communityId.isEmpty && event.organisationId.isEmpty && event.systemId.isEmpty) ||
      (
        state.hasData() && (
            (event.communityId.nonEmpty && event.communityId.get == state.community()) ||
            (event.organisationId.nonEmpty && event.organisationId.get == state.organisation()) ||
            (event.systemId.nonEmpty && event.systemId.get == state.system())
        )
      )
  }

  private def loadTestCaseDefinition(testCaseId: Long, state: SessionLaunchState): (TestCase, SessionLaunchState) = {
    var testCaseDefinition = state.testCaseDefinition(testCaseId)
    var stateToReturn = state
    if (testCaseDefinition.isEmpty) {
      testCaseDefinition = Some(testbedBackendClient.getTestCaseDefinition(testCaseId.toString, None, getSessionConfigurationData(stateToReturn, onlySimple = true)).getTestcase)
      stateToReturn = replace(state.newForLoadedTestCaseDefinition(testCaseId, testCaseDefinition.get))
    }
    (testCaseDefinition.get, stateToReturn)
  }

  private def replace(newState: SessionLaunchState): SessionLaunchState = {
    context.become(active(newState), discardOld = true)
    newState
  }

  private def getSessionConfigurationData(state: SessionLaunchState, onlySimple: Boolean): SessionConfigurationData = {
    if (onlySimple) {
      // Include only the configuration values that are simple texts
      SessionConfigurationData(
        Some(state.data.get.statementParameters.map { x =>
          TypedActorConfiguration(x.actor, x.endpoint, x.config.filter(_.kind == "SIMPLE"))
        }),
        state.data.get.domainParameters.map { x =>
          TypedActorConfiguration(x.actor, x.endpoint, x.config.filter(_.kind == "SIMPLE"))
        },
        Some(TypedActorConfiguration(state.data.get.organisationParameters.actor, state.data.get.organisationParameters.endpoint, state.data.get.organisationParameters.config.filter(_.kind == "SIMPLE"))),
        Some(TypedActorConfiguration(state.data.get.systemParameters.actor, state.data.get.systemParameters.endpoint, state.data.get.systemParameters.config.filter(_.kind == "SIMPLE")))
      )
    } else {
      // Include all configuration values
      SessionConfigurationData(
        Some(state.data.get.statementParameters),
        state.data.get.domainParameters,
        Some(state.data.get.organisationParameters),
        Some(state.data.get.systemParameters)
      )
    }
  }

  private def processNextTestSession(initialState: SessionLaunchState): Unit = {
    var latestState = initialState
    if (latestState.hasSessionsToProcess()) {
      var scheduleNextSessionLaunch = true
      try {
        val testCaseId = latestState.nextTestCaseId()
        val testCaseDefinitionLoad = loadTestCaseDefinition(testCaseId, latestState)
        latestState = testCaseDefinitionLoad._2
        if (latestState.testCaseAllowedToExecute(testCaseId)) {
          var testSessionId: Option[String] = None
          try {
            val sessionIdToAssign = latestState.assignPredefinedSessionId(testCaseId)
            testSessionId = Some(testbedBackendClient.initiate(testCaseId, sessionIdToAssign))
            latestState = replace(latestState.newForConfiguredTestSession(testCaseId, testSessionId.get))
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Configuring test session [{}] for test case [{}]. {}", testSessionId.get, testCaseDefinitionLoad._1.getId, latestState.statusText())
            webSocketActor.registerActiveTestSession(testSessionId.get)
            // Send the configure request. The response will be returned asynchronously.
            testbedBackendClient.configure(testSessionId.get, getSessionConfigurationData(latestState, onlySimple = false), latestState.testCaseInputs(testCaseId))
          } catch {
            case e: Exception =>
              if (testSessionId.isEmpty) {
                // Error in the initiate call.
                LOGGER.error("A headless session for test case [{}] raised an uncaught error while being configured", testCaseDefinitionLoad._1.getId, e)
                replace(latestState.newForFailedTestCase(testCaseId))
              } else {
                // Error in the configure call.
                LOGGER.error("A headless session [{}] for test case [{}] raised an uncaught error while being configured", testSessionId, testCaseDefinitionLoad._1.getId, e)
                replace(latestState.newForCompletedTestSession(testSessionId.get))
                webSocketActor.removeActiveTestSession(testSessionId.get)
              }
          }
        } else {
          if (LOGGER.isDebugEnabled()) LOGGER.debug("Waiting for other sessions to complete before launching test case [{}]", testCaseDefinitionLoad._1.getId)
          scheduleNextSessionLaunch = false
        }
      } finally {
        if (scheduleNextSessionLaunch) {
          self ! ProcessNextTestSessionEvent()
        }
      }
    } else if (!latestState.allSessionsStarted()) {
      // No test sessions to configure, but we haven't yet started all pending sessions
      if (LOGGER.isDebugEnabled()) LOGGER.debug("Waiting for all sessions to start. {}", latestState.statusText())
    } else {
      // Nothing to do - stop the actor
      if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch finished. {}", latestState.statusText())
      self ! PoisonPill.getInstance
    }
  }

  private def sessionConfigured(msg: TestSessionConfiguredEvent, initialState: SessionLaunchState): Unit = {
    val testSessionId = msg.event.getTcInstanceId
    var scheduleNextSessionLaunch = false
    var latestState = initialState
    try {
      if (msg.event.getErrorCode == null) {
        // No error - configuration was successful.
        val testCaseId = initialState.sessionIdMap(testSessionId)
        val testCaseDefinitionData = loadTestCaseDefinition(testCaseId, initialState)
        val testCaseDefinition = testCaseDefinitionData._1
        latestState = replace(testCaseDefinitionData._2.newForStartedTestSession(testSessionId))
        // No need to signal any simulated configurations (the result of the configure step) as there is no one to present these to.
        // Preliminary step is skipped as this is a headless session. If input was expected during this step the test session may fail.
        reportManager.createTestReport(testSessionId, latestState.system(), testCaseId.toString, latestState.actor(), testCaseDefinition)
        triggerHelper.publishTriggerEvent(new TestSessionStartedEvent(latestState.community(), testSessionId))
        testbedBackendClient.start(testSessionId)
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Launching test session [{}] for test case [{}]. {}", testSessionId, testCaseDefinition.getId, latestState.statusText())
      } else {
        LOGGER.info("Configuration failure of test session [{}]", testSessionId)
        latestState = replace(latestState.newForCompletedTestSession(testSessionId))
        scheduleNextSessionLaunch = true
        webSocketActor.removeActiveTestSession(testSessionId)
      }
    } catch {
      case e: Exception =>
        LOGGER.error("Headless session [{}] raised an uncaught error while being started", msg.event.getTcInstanceId, e)
        replace(latestState.newForCompletedTestSession(testSessionId))
        scheduleNextSessionLaunch = true
        webSocketActor.removeActiveTestSession(testSessionId)
    } finally {
      if (scheduleNextSessionLaunch) {
        // In case of a problem make sure we signal the next test case to be processed so that we're not stuck.
        self ! ProcessNextTestSessionEvent()
      }
    }
  }

}
