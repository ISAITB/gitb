package actors

import actors.events.sessions.TestSessionCompletedEvent
import com.gitb.core.ValueEmbeddingEnumeration
import com.gitb.tbs.{Instruction, InteractWithUsersRequest, TestStepStatus}
import com.gitb.tr.TAR
import managers.{ReportManager, TestResultManager, TestbedBackendClient}
import models.{TestInteraction, TestStepResultInfo}
import org.apache.commons.lang3.StringUtils
import org.apache.pekko.actor.{Actor, PoisonPill}
import org.slf4j.LoggerFactory
import utils._

import java.nio.file.Files
import javax.inject.Inject
import scala.jdk.CollectionConverters.CollectionHasAsScala

object SessionUpdateActor {
  trait Factory {
    def apply(): Actor
  }
}

class SessionUpdateActor @Inject() (repositoryUtils: RepositoryUtils, reportManager: ReportManager, testResultManager: TestResultManager, webSocketActor: WebSocketActor, testbedBackendClient: TestbedBackendClient) extends Actor {

  private val LOGGER = LoggerFactory.getLogger(classOf[SessionUpdateActor])
  private val END_STEP_ID = "-1"
  private val END_STOP_STEP_ID = "-2"
  private val LOG_EVENT_STEP_ID = "-999"

  override def preStart():Unit = {
    LOGGER.debug(s"Starting session update actor [${self.path.name}]")
    super.preStart()
  }

  override def postStop(): Unit = {
    LOGGER.debug(s"Stopping session update actor [${self.path.name}]")
    super.postStop()
  }

  override def receive: Receive = {
    case msg: TestStepStatus => updateStatus(msg)
    case msg: InteractWithUsersRequest => interactWithUsers(msg)
    case msg: Object =>
      LOGGER.warn(s"Session update actor received unexpected message [${msg.getClass.getName}]")
  }

  private def updateStatus(testStepStatus: TestStepStatus): Unit = {
    try {
      val session: String = testStepStatus.getTcInstanceId
      val step: String = testStepStatus.getStepId
      // Save report
      if (step == END_STEP_ID || step == END_STOP_STEP_ID) {
        try {
          var outputMessage: String = null
          testStepStatus.getReport match {
            case tar: TAR if tar.getContext != null && tar.getContext.getValue != null && !tar.getContext.getValue.isBlank =>
              outputMessage = tar.getContext.getValue.trim
            case _ => // Do nothing
          }
          reportManager.finishTestReport(session, testStepStatus.getReport.getResult, Option(outputMessage))
          val statusUpdates: List[(String, TestStepResultInfo)] = testResultManager.sessionRemove(session)
          val resultInfo: TestStepResultInfo = new TestStepResultInfo(testStepStatus.getStatus.ordinal.toShort, None)
          val message: String = JsonUtil.jsTestStepResultInfo(session, step, resultInfo, Option(outputMessage), statusUpdates).toString
          webSocketActor.testSessionEnded(session, message)
        } finally {
          // Notify that a test session has completed.
          context.system.eventStream.publish(TestSessionCompletedEvent(session))
          // Stop the current actor
          self ! PoisonPill.getInstance
        }
      } else {
        if (step == LOG_EVENT_STEP_ID) { //send log event
          testStepStatus.getReport match {
            case tar: TAR if tar.getContext != null =>
              val logMessage: String = tar.getContext.getValue
              testResultManager.sessionUpdate(session, logMessage)
            case _ =>
          }
          webSocketActor.broadcast(session, JacksonUtil.serializeTestStepStatus(testStepStatus), retry = false)
        } else {
          val reportPath: Option[String] = reportManager.createTestStepReport(session, testStepStatus)
          val resultInfo: TestStepResultInfo = new TestStepResultInfo(testStepStatus.getStatus.ordinal.toShort, reportPath)
          val statusUpdates: List[(String, TestStepResultInfo)] = testResultManager.sessionUpdate(session, step, resultInfo)
          val message: String = JsonUtil.jsTestStepResultInfo(session, step, resultInfo, Option.empty, statusUpdates).toString
          webSocketActor.broadcast(session, message)
        }
      }
    } catch {
      case e: Exception =>
        LOGGER.error(s"Error during test session update for session [${testStepStatus.getTcInstanceId}]", e)
    }
  }

  private def interactWithUsers(interactWithUsersRequest: InteractWithUsersRequest): Unit = {
    val session = interactWithUsersRequest.getTcInstanceid
    try {
      if (interactWithUsersRequest.getInteraction != null) {
        val sessionFolder = repositoryUtils.getPathForTestSession(interactWithUsersRequest.getTcInstanceid, isExpected = false).path
        Files.createDirectories(sessionFolder)
        interactWithUsersRequest.getInteraction.getInstructionOrRequest.asScala.foreach {
          case instruction: Instruction if !StringUtils.isBlank(instruction.getValue) && instruction.getEmbeddingMethod == ValueEmbeddingEnumeration.BASE_64 && StringUtils.isBlank(instruction.getName) => // Determine the file name from the BASE64 content.
            val mimeType = MimeUtil.getMimeType(instruction.getValue, false)
            val extension = MimeUtil.getExtensionFromMimeType(mimeType)
            // Determine name.
            if (extension != null) {
              instruction.setName(s"file$extension")
            }
            // Decouple large content if needed into file references.
            repositoryUtils.decoupleLargeData(instruction, sessionFolder, isTempData = true)
          case _ => // Ignoring requests.
        }
        val request = JacksonUtil.serializeInteractionRequest(interactWithUsersRequest)
        testResultManager.saveTestInteraction(TestInteraction(interactWithUsersRequest.getTcInstanceid, interactWithUsersRequest.getStepId, interactWithUsersRequest.getInteraction.isAdmin, TimeUtil.getCurrentTimestamp(), request))
        if (WebSocketActor.webSockets.contains(session)) {
          val actor = interactWithUsersRequest.getInteraction.getWith
          if (actor == null) { // if actor not specified, send the request to all actors. Let client side handle this.
            webSocketActor.broadcast(session, request)
          } else { //send the request only to the given actor
            webSocketActor.push(session, actor, request)
          }
        } else {
          // This is a headless session.
          if (!interactWithUsersRequest.getInteraction.isAdmin && !interactWithUsersRequest.getInteraction.isHasTimeout) {
            // This is a non-admin interaction for which we don't have a timeout defined - resolve the step immediately with empty inputs.
            // Admin interactions are not force-completed like this because they will normally always be done asynchronously.
            // They remain active indefinitely or until a configured timeout expires (handled via gitb-srv).
            LOGGER.warn(s"Headless session [$session] expected interaction for step [${interactWithUsersRequest.getStepId}]. Completed automatically with empty result.")
            testbedBackendClient.provideInput(session, interactWithUsersRequest.getStepId, None, interactWithUsersRequest.getInteraction.isAdmin)
          }
        }
      } else {
        LOGGER.warn(s"Session [$session] received empty interaction for step [${interactWithUsersRequest.getStepId}].")
        testbedBackendClient.provideInput(session, interactWithUsersRequest.getStepId, None, interactWithUsersRequest.getInteraction.isAdmin)
      }
    } catch {
      case e: Exception =>
        LOGGER.error(s"Error during user interaction for session [$session]", e)
    }
  }

}
