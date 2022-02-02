package actors

import actors.events.sessions.{PrepareTestSessionsEvent, SessionLaunchState, StartNextTestSessionEvent, TerminateAllSessionsEvent, TestSessionCompletedEvent}
import akka.actor.{Actor, PoisonPill}
import managers.{ReportManager, TestbedBackendClient}
import org.slf4j.LoggerFactory

import javax.inject.Inject

object SessionLaunchActor {
  trait Factory {
    def apply(): Actor
  }
}

class SessionLaunchActor @Inject() (reportManager: ReportManager, testbedBackendClient: TestbedBackendClient, webSocketActor: WebSocketActor) extends Actor {

  private val LOGGER = LoggerFactory.getLogger(classOf[SessionLaunchActor])

  override def preStart():Unit = {
    super.preStart()
    context.system.eventStream.subscribe(context.self, classOf[TestSessionCompletedEvent])
    context.system.eventStream.subscribe(context.self, classOf[TerminateAllSessionsEvent])
    LOGGER.debug("Session launch actor started ["+self.path+"]")
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(context.self)
    LOGGER.debug("Session launch actor stopped ["+self.path+"]")
  }

  override def receive: Receive = active(SessionLaunchState(None, Set.empty, Set.empty, Map.empty))

  def active(state: SessionLaunchState): Receive = {
    case msg: PrepareTestSessionsEvent =>
      context.become(active(SessionLaunchState(Some(msg.launchData), Set.empty, Set.empty, Map.empty)))
      self ! StartNextTestSessionEvent()
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

  private def startNextTestSession(initialState: SessionLaunchState): Unit = {
    if (initialState.data.get.testCases.isEmpty) {
      // Nothing to do - stop the actor
      self ! PoisonPill.getInstance
    } else {
      var state = initialState
      val testCaseId = state.data.get.testCases.head
      var testCaseDefinition = state.testCaseDefinitionCache.get(testCaseId)
      if (testCaseDefinition.isEmpty) {
        testCaseDefinition = Some(testbedBackendClient.getTestCaseDefinition(testCaseId.toString, None).getTestcase)
        state = state.newWithLoadedTestCaseDefinition(testCaseId, testCaseDefinition.get)
        context.become(active(state), discardOld = true)
      }
      var proceedToLaunch = false
      var scheduleNextSessionLaunch = true
      if (state.hasActiveTestSessions()) {
        // Other sessions are ongoing
        if (state.data.get.forceSequentialExecution) {
          // We want to execute sequentially so we won't launch the next test session (we'll re-check when the next session ends).
          LOGGER.debug("Session launch actor ["+self.path+"] waiting for other sessions to complete before starting test case ["+testCaseId+"] because launch is forced sequential")
          scheduleNextSessionLaunch = false
        } else {
          if (testCaseDefinition.get.isSupportsParallelExecution) {
            proceedToLaunch = true
          } else {
            // Test case is sequential so we won't launch it (we'll re-check when the next session ends).
            LOGGER.debug("Session launch actor ["+self.path+"] waiting for other sessions to complete before starting test case ["+testCaseId+"] because the test case is sequential")
            scheduleNextSessionLaunch = false
          }
        }
      } else {
        // No other session is running - proceed
        proceedToLaunch = true
        if (!testCaseDefinition.get.isSupportsParallelExecution) {
          scheduleNextSessionLaunch = false
        }
      }
      if (proceedToLaunch) {
        val testSessionId = testbedBackendClient.initiate(testCaseId)
        webSocketActor.testSessionStarted(testSessionId)
        try {
          testbedBackendClient.configure(testSessionId, state.data.get.statementParameters, state.data.get.domainParameters, state.data.get.organisationParameters, state.data.get.systemParameters)
          // Preliminary step is skipped as this is a headless session. If input was expected during this step the test session will fail.
          reportManager.createTestReport(testSessionId, state.data.get.systemId, testCaseId.toString, state.data.get.actorId, testCaseDefinition.get)
          testbedBackendClient.start(testSessionId)
          LOGGER.debug("Session launch actor ["+self.path+"] started test session ["+testSessionId+"] for test case ["+testCaseId+"]")
          context.become(active(state.newForStartedTestSession(testCaseId, testSessionId)), discardOld = true)
        } catch {
          case e: Exception =>
            LOGGER.error("A headless session raised an uncaught error while being started", e)
            webSocketActor.testSessionEnded(testSessionId)
        } finally {
          if (scheduleNextSessionLaunch) {
            // Having started the current session we will also signal to start the next one (if available).
            self ! StartNextTestSessionEvent()
          }
        }
      }
    }
  }

}
