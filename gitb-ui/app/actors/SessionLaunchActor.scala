package actors

import actors.SessionLaunchActor._
import actors.events.TestSessionStartedEvent
import actors.events.sessions._
import com.gitb.tpl.TestCase
import managers.triggers.TriggerHelper
import managers.{ReportManager, TestbedBackendClient}
import org.apache.pekko.actor.Status.Failure
import org.apache.pekko.actor.{Actor, PoisonPill}
import org.slf4j.LoggerFactory

import javax.inject.Inject
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object SessionLaunchActor {

  trait Factory {
    def apply(): Actor
  }

  private case class PrepareTestSessionsEventWrapper(wrapped: PrepareTestSessionsEvent)
  private case class TestSessionConfiguredEventWrapper(wrapped: TestSessionConfiguredEvent)
  private case class TestSessionCompletedEventWrapper(wrapped: TestSessionCompletedEvent)
  private case class TerminateSessionsEventWrapper(wrapped: TerminateSessionsEvent)
  private case class TerminateAllSessionsEventWrapper(wrapped: TerminateAllSessionsEvent)

  private case class ProcessNextTestSessionEvent()
  private case class TaskCompleted(message: Option[Any])
  private case class TestCaseDefinitionLoaded(testCaseId: Long, testCaseDefinition: TestCase)
  private case class TestSessionInitialised(testCaseId: Long, assignedTestSession: String)
  private case class TestSessionError(cause: Throwable, testCaseId: Long)

}

class SessionLaunchActor @Inject() (reportManager: ReportManager,
                                    testbedBackendClient: TestbedBackendClient,
                                    webSocketActor: WebSocketActor,
                                    triggerHelper: TriggerHelper)
                                   (implicit ec: ExecutionContext) extends Actor {

  private val LOGGER = LoggerFactory.getLogger(classOf[SessionLaunchActor])

  /*
   * As long as the actor's state is mutated strictly within the actor's own thread (i.e., not externally and not in futures),
   * we can define this as mutable class properties with no risk of concurrency issues.
   */
  private val state = new SessionLaunchState()
  private var stopping = false
  // We use a queue for any asynchronous work needed to ensure that all asynchronous tasks are processed in sequence.
  private val workQueue = mutable.Queue[() => Future[TaskCompleted]]()
  private var workQueueProcessing = false

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

  override def receive: Receive = {
    /*
     * Wrap externally received events as self-posted messages. We do this to make sure that
     * messages are processed in full sequence with respect to the actor's other self-posted messages.
     */
    case msg: PrepareTestSessionsEvent => self ! PrepareTestSessionsEventWrapper(msg)
    case msg: TestSessionConfiguredEvent => self ! TestSessionConfiguredEventWrapper(msg)
    case msg: TestSessionCompletedEvent => self ! TestSessionCompletedEventWrapper(msg)
    case msg: TerminateSessionsEvent => self ! TerminateSessionsEventWrapper(msg)
    case msg: TerminateAllSessionsEvent => self ! TerminateAllSessionsEventWrapper(msg)
    /*
     * Process messages to adapt the state of the launched sessions.
     */
    case msg: PrepareTestSessionsEventWrapper =>
      state.setLaunchData(msg.wrapped.launchData)
      self ! ProcessNextTestSessionEvent()
    case msg: TestSessionConfiguredEventWrapper =>
      if (!stopping) {
        if (state.isConfiguredSession(msg.wrapped.event.getTcInstanceId)) {
          testSessionConfigured(msg.wrapped)
        } else {
          if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] - received a configuration complete notification for an unknown test session [{}]. {}", self.path, msg.wrapped.event.getTcInstanceId, state.statusText())
        }
      }
    case msg: TestSessionCompletedEventWrapper =>
      if (!stopping) {
        if (state.isStartedSession(msg.wrapped.testSession)) {
          testSessionCompleted(msg.wrapped)
        } else {
          if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] received a test session complete notification for an unknown test session [{}]", self.path, msg.wrapped.testSession)
        }
      }
    case msg: TerminateSessionsEventWrapper =>
      if (!stopping) {
        if (state.matchesOrganisation(msg.wrapped.organisationId)) {
          terminateSessions(msg.wrapped)
        } else {
          if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] received a termination request related to another organisation [{}] (expected [{}])", self.path, msg.wrapped.organisationId, state.organisation())
        }
      }
    case msg: TerminateAllSessionsEventWrapper =>
      if (!stopping) {
        if (terminationApplies(msg.wrapped, state)) {
          if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] skipping remaining test sessions", self.path)
          stopping = true
          self ! PoisonPill.getInstance
        }
      }
    case msg: TestCaseDefinitionLoaded =>
      if (!stopping) {
        if (state.isLoadingDefinition(msg.testCaseId)) {
          testCaseDefinitionLoaded(msg)
        } else {
          if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] received a test case definition notification for an unknown test case [{}]", self.path, msg.testCaseId)
        }
      }
    case msg: TestSessionInitialised =>
      if (!stopping) {
        if (state.isInProgress(msg.testCaseId)) {
          testSessionInitialised(msg)
        } else {
          if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch actor [{}] received a test session initialisation notification for an unknown test session [{}]", self.path, msg.assignedTestSession)
        }
      }
    case _: ProcessNextTestSessionEvent =>
      if (!stopping) {
        processNextTestSession()
      }
    case msg: TaskCompleted =>
      if (!stopping) {
        workQueueProcessing = false
        // If the task was bearing a message send it.
        msg.message.foreach(self ! _)
        // Continue processing the queue.
        processQueue()
      }
    case msg: TestSessionError =>
      if (!stopping) {
        handleError(msg)
      }
    case msg: Failure =>
      LOGGER.warn("Session launch actor caught unexpected error", msg.cause)
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

  private def handleError(msg: TestSessionError): Unit = {
    val testSessionId = state.sessionForTestCaseId(msg.testCaseId)
    LOGGER.error("Headless session [{}] for test case [{}] raised an uncaught error while being started", testSessionId.getOrElse("-"), msg.testCaseId, msg.cause)
    state.setFailedTestCase(msg.testCaseId)
    if (testSessionId.isDefined) {
      webSocketActor.removeActiveTestSession(testSessionId.get)
    }
    self ! ProcessNextTestSessionEvent()
  }

  private def terminateSessions(msg: TerminateSessionsEvent): Unit = {
    val count = state.removeTestSessions(msg.testSessions)
    if (count > 0) {
      LOGGER.info("Cancelled {} test session(s) that were pending execution", count)
      // Checking to start another session would normally take place after we received a configuration complete event.
      self ! ProcessNextTestSessionEvent()
    }
  }

  private def testSessionCompleted(msg: TestSessionCompletedEvent): Unit = {
    state.setCompletedTestSession(msg.testSession)
    if (LOGGER.isDebugEnabled()) LOGGER.debug("Completed session [{}]. {}", msg.testSession, state.statusText())
    self ! ProcessNextTestSessionEvent()
  }

  private def testSessionConfigured(msg: TestSessionConfiguredEvent): Unit = {
    val testSessionId = msg.event.getTcInstanceId
    if (msg.event.getErrorCode == null) {
      // No error - configuration was successful.
      val testCaseIdOpt = state.testCaseIdForSession(testSessionId)
      if (testCaseIdOpt.isDefined) {
        val testCaseId = testCaseIdOpt.get
        val testCaseDefinitionOpt = state.testCaseDefinition(testCaseId)
        if (testCaseDefinitionOpt.isDefined) {
          val testCaseDefinition = testCaseDefinitionOpt.get
          // No need to signal any simulated configurations (the result of the configure step) as there is no one to present these to.
          // Preliminary step is skipped as this is a headless session. If input was expected during this step the test session may fail.
          state.setStartedTestSession(testSessionId)
          if (LOGGER.isDebugEnabled()) LOGGER.debug("Launching test session [{}] for test case [{}]. {}", testSessionId, testCaseDefinition.getId, state.statusText())
          queueTask(() => {
            reportManager.createTestReport(testSessionId, state.system(), testCaseId.toString, state.actor(), testCaseDefinition).flatMap { _ =>
              testbedBackendClient.start(testSessionId).map { _ =>
                triggerHelper.publishTriggerEvent(new TestSessionStartedEvent(state.community(), testSessionId))
                TaskCompleted(None)
              }
            }.recover {
              case e: Exception =>
                TaskCompleted(Some(TestSessionError(e, testCaseId)))
            }
          })
        } else {
          // Illegal state
          LOGGER.error("Missing expected test case definition for test session [{}]", testSessionId)
          markSessionAsFailed(testSessionId)
        }
      } else {
        // Illegal state
        LOGGER.error("Missing expected test case ID for test session [{}]", testSessionId)
        markSessionAsFailed(testSessionId)
      }
    } else {
      LOGGER.info("Configuration failure of test session [{}]", testSessionId)
      markSessionAsFailed(testSessionId)
    }
  }

  private def markSessionAsFailed(testSessionId: String): Unit = {
    state.setFailedTestSession(testSessionId)
    webSocketActor.removeActiveTestSession(testSessionId)
    self ! ProcessNextTestSessionEvent()
  }

  private def testSessionInitialised(msg: TestSessionInitialised): Unit = {
    state.setConfiguredTestSession(msg.testCaseId, msg.assignedTestSession)
    val testCaseInputs = state.testCaseInputs(msg.testCaseId)
    val testCaseConfiguration = state.getSessionConfigurationData(onlySimple = false)
    if (LOGGER.isDebugEnabled()) LOGGER.debug("Initiated test session [{}] for test case [{}]. {}", msg.assignedTestSession, msg.testCaseId, state.statusText())
    webSocketActor.registerActiveTestSession(msg.assignedTestSession)
    // Send the configure request. The response will be returned asynchronously.
    queueTask(() => {
      testbedBackendClient.configure(msg.assignedTestSession, testCaseConfiguration, testCaseInputs).map { _ =>
        TaskCompleted(None)
      }.recover {
        case e: Exception =>
          TaskCompleted(Some(TestSessionError(e, msg.testCaseId)))
      }
    })
  }

  private def testCaseDefinitionLoaded(msg: TestCaseDefinitionLoaded): Unit = {
    state.setTestCaseDefinition(msg.testCaseId, msg.testCaseDefinition)
    if (state.testCaseAllowedToExecute(msg.testCaseId)) {
      state.setInProgressTestSession(msg.testCaseId)
      val sessionIdToAssign = state.assignPredefinedSessionId(msg.testCaseId)
      queueTask(() => {
        testbedBackendClient.initiate(msg.testCaseId, sessionIdToAssign).map { result =>
          TaskCompleted(Some(TestSessionInitialised(msg.testCaseId, result)))
        }
      }.recover {
        case e: Exception =>
          TaskCompleted(Some(TestSessionError(e, msg.testCaseId)))
      })
      self ! ProcessNextTestSessionEvent()
    } else {
      if (LOGGER.isDebugEnabled()) LOGGER.debug("Waiting for other sessions to complete before launching test case [{}]", msg.testCaseId)
    }
  }

  private def processNextTestSession(): Unit = {
    if (state.hasSessionsToProcess()) {
      val testCaseId = state.nextTestCaseId()
      if (state.testCaseAllowedToExecute(testCaseId)) {
        state.setLoadingDefinitionForTestCase(testCaseId)
        val configurationData = state.getSessionConfigurationData(onlySimple = true)
        queueTask(() => {
          testbedBackendClient.getTestCaseDefinition(testCaseId.toString, None, configurationData).map { result =>
            TaskCompleted(Some(TestCaseDefinitionLoaded(testCaseId, result.getTestcase)))
          }.recover {
            case e: Exception =>
              TaskCompleted(Some(TestSessionError(e, testCaseId)))
          }
        })
      } else {
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Waiting for other sessions to complete before initialising test case [{}]", testCaseId)
      }
    } else if (!stopping && (!state.allSessionsStarted() || workQueue.nonEmpty)) {
      // No test sessions to configure, but we haven't yet started all pending sessions
      if (LOGGER.isDebugEnabled()) LOGGER.debug("Waiting for all sessions to start. {}", state.statusText())
    } else {
      // Nothing to do - stop the actor
      if (LOGGER.isDebugEnabled()) LOGGER.debug("Session launch finished. {}", state.statusText())
      self ! PoisonPill.getInstance
    }
  }

  private def processQueue(): Unit = {
    if (!workQueueProcessing && workQueue.nonEmpty) {
      workQueueProcessing = true
      val task = workQueue.dequeue()
      task().foreach(self ! _)  // When the Future completes, send message to self
    }
  }

  private def queueTask(task: () => Future[TaskCompleted]): Unit = {
    workQueue.enqueue(task)
    processQueue()
  }

}
