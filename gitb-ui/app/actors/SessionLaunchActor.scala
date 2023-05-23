package actors

import actors.events.TestSessionStartedEvent
import actors.events.sessions.{PrepareTestSessionsEvent, SessionLaunchState, StartNextTestSessionEvent, TerminateAllSessionsEvent, TestSessionCompletedEvent, TestSessionConfiguredEvent}
import akka.actor.{Actor, PoisonPill}
import com.gitb.core.AnyContent
import com.gitb.tpl.TestCase
import managers.{ReportManager, TestbedBackendClient, TriggerHelper}
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
    LOGGER.debug("Session launch actor started ["+self.path+"]")
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(context.self)
    LOGGER.debug("Session launch actor stopped ["+self.path+"]")
  }

  override def receive: Receive = active(SessionLaunchState(None, Map.empty, Set.empty, Map.empty))

  def active(state: SessionLaunchState): Receive = {
    case msg: PrepareTestSessionsEvent =>
      context.become(active(SessionLaunchState(Some(msg.launchData), Map.empty, Set.empty, Map.empty)))
      self ! StartNextTestSessionEvent()
    case msg: TestSessionConfiguredEvent =>
      if (state.startedTestSessions.contains(msg.event.getTcInstanceId)) {
        sessionConfigured(msg, state)
      } else {
        LOGGER.warn("Session launch actor ["+self.path+"] received a configuration complete notification for an unknown test session ["+msg.event.getTcInstanceId+"]")
      }
    case msg: TestSessionCompletedEvent =>
      if (state.startedTestSessions.contains(msg.testSession)) {
        context.become(active(state.newWithCompletedTestSession(msg.testSession)), discardOld = true)
        LOGGER.debug("Session launch actor ["+self.path+"] recorded completion of test session ["+msg.testSession+"]")
        self ! StartNextTestSessionEvent()
      }
    case _: StartNextTestSessionEvent =>
      startNextTestSession(state)
    case msg: TerminateAllSessionsEvent =>
      if (terminationApplies(msg, state)) {
        LOGGER.debug("Session launch actor ["+self.path+"] skipping remaining test sessions")
        self ! PoisonPill.getInstance
      }
    case msg: Object =>
      LOGGER.warn(s"Session launch actor received unexpected message [${msg.getClass.getName}]")
  }

  private def terminationApplies(event: TerminateAllSessionsEvent, state: SessionLaunchState): Boolean = {
    (event.communityId.isEmpty && event.organisationId.isEmpty && event.systemId.isEmpty) ||
      (event.communityId.nonEmpty && state.data.nonEmpty && event.communityId.get == state.data.get.communityId) ||
      (event.organisationId.nonEmpty && state.data.nonEmpty && event.organisationId.get == state.data.get.organisationId) ||
      (event.systemId.nonEmpty && state.data.nonEmpty && event.systemId.get == state.data.get.systemId)
  }

  private def loadTestCaseDefinition(testCaseId: Long, state: SessionLaunchState): (TestCase, SessionLaunchState) = {
    var testCaseDefinition = state.testCaseDefinitionCache.get(testCaseId)
    var stateToReturn = state
    if (testCaseDefinition.isEmpty) {
      testCaseDefinition = Some(testbedBackendClient.getTestCaseDefinition(testCaseId.toString, None).getTestcase)
      stateToReturn = state.newWithLoadedTestCaseDefinition(testCaseId, testCaseDefinition.get)
      context.become(active(stateToReturn), discardOld = true)
    }
    (testCaseDefinition.get, stateToReturn)
  }

  private def startNextTestSession(initialState: SessionLaunchState): Unit = {
    if (initialState.data.get.testCases.isEmpty) {
      // Nothing to do - stop the actor
      self ! PoisonPill.getInstance
    } else {
      val testCaseId = initialState.data.get.testCases.head
      val testCaseDefinitionLoad = loadTestCaseDefinition(testCaseId, initialState)
      val testCaseDefinition = testCaseDefinitionLoad._1
      val state = testCaseDefinitionLoad._2
      var sessionIdToAssign: Option[String] = None
      if (state.data.get.sessionIdsToAssign.isDefined) {
        sessionIdToAssign = state.data.get.sessionIdsToAssign.get.get(testCaseId)
      }
      var proceedToLaunch = false
      var scheduleNextSessionLaunch = true
      if (state.hasActiveTestSessions()) {
        // Other sessions are ongoing
        if (state.data.get.forceSequentialExecution) {
          // We want to execute sequentially so we won't launch the next test session (we'll re-check when the next session ends).
          LOGGER.debug("Session launch actor ["+self.path+"] waiting for other sessions to complete before configuring test case ["+testCaseId+"] because launch is forced sequential")
          scheduleNextSessionLaunch = false
        } else {
          if (testCaseDefinition.isSupportsParallelExecution) {
            proceedToLaunch = true
          } else {
            // Test case is sequential so we won't launch it (we'll re-check when the next session ends).
            LOGGER.debug("Session launch actor ["+self.path+"] waiting for other sessions to complete before configuring test case ["+testCaseId+"] because the test case is sequential")
            scheduleNextSessionLaunch = false
          }
        }
      } else {
        // No other session is running - proceed
        proceedToLaunch = true
        if (!testCaseDefinition.isSupportsParallelExecution) {
          scheduleNextSessionLaunch = false
        }
      }
      if (proceedToLaunch) {
        val testSessionId = testbedBackendClient.initiate(testCaseId, sessionIdToAssign)
        webSocketActor.testSessionStarted(testSessionId)
        var testCaseInputs: Option[List[AnyContent]] = None
        if (state.data.get.testCaseToInputMap.isDefined) {
          testCaseInputs = state.data.get.testCaseToInputMap.get.get(testCaseId)
        }
        try {
          // Register the session ID so that we pick up its configuration ready event.
          context.become(active(state.newForStartedTestSession(testCaseId, testSessionId)), discardOld = true)
          // Send the configure request. The response will be returned asynchronously.
          testbedBackendClient.configure(testSessionId, state.data.get.statementParameters, state.data.get.domainParameters, state.data.get.organisationParameters, state.data.get.systemParameters, testCaseInputs)
        } catch {
          case e: Exception =>
            LOGGER.error("A headless session raised an uncaught error while being configured", e)
            scheduleNextSessionLaunch = false
            webSocketActor.testSessionEnded(testSessionId)
        }
      }
    }
  }

  private def sessionConfigured(msg: TestSessionConfiguredEvent, initialState: SessionLaunchState): Unit = {
    var scheduleNextSessionLaunch = true
    val testSessionId = msg.event.getTcInstanceId
    try {
      if (msg.event.getErrorCode == null) {
        // No error - configuration was successful.
        val testCaseId = initialState.startedTestSessions(testSessionId)
        val testCaseDefinitionData = loadTestCaseDefinition(testCaseId, initialState)
        val testCaseDefinition = testCaseDefinitionData._1
        val state = testCaseDefinitionData._2
        // No need to signal any simulated configurations (the result of the configure step) as there is no one to present these to.
        // Preliminary step is skipped as this is a headless session. If input was expected during this step the test session may fail.
        reportManager.createTestReport(testSessionId, state.data.get.systemId, testCaseId.toString, state.data.get.actorId, testCaseDefinition)
        triggerHelper.publishTriggerEvent(new TestSessionStartedEvent(state.data.get.communityId, testSessionId))
        testbedBackendClient.start(testSessionId)
        LOGGER.debug("Session launch actor ["+self.path+"] started test session ["+testSessionId+"] for test case ["+testCaseId+"]")
      } else {
        context.become(active(initialState.newWithCompletedTestSession(testSessionId)), discardOld = true)
        LOGGER.info("Session launch actor ["+self.path+"] notified for configuration failure of test session ["+testSessionId+"]")
      }
    } catch {
      case e: Exception =>
        LOGGER.error("A headless session raised an uncaught error while being started", e)
        scheduleNextSessionLaunch = false
        webSocketActor.testSessionEnded(testSessionId)
    } finally {
      if (scheduleNextSessionLaunch) {
        // Having started the current session we will also signal to start the next one (if available).
        self ! StartNextTestSessionEvent()
      }
    }
  }

}
