package actors

import akka.actor.{Actor, PoisonPill}
import com.gitb.core.ValueEmbeddingEnumeration
import com.gitb.tbs.{Instruction, InteractWithUsersRequest, ProvideInputRequest, TestStepStatus}
import com.gitb.tr.TAR
import managers.{ReportManager, TestResultManager, TestbedBackendClient}
import models.TestStepResultInfo
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import utils.{JacksonUtil, JsonUtil, MimeUtil}

import javax.inject.Inject
import scala.jdk.CollectionConverters.CollectionHasAsScala

object SessionUpdateActor {
  trait Factory {
    def apply(): Actor
  }
}

class SessionUpdateActor @Inject() (reportManager: ReportManager, testResultManager: TestResultManager, webSocketActor: WebSocketActor, testbedBackendClient: TestbedBackendClient) extends Actor {

  private val LOGGER = LoggerFactory.getLogger(classOf[SessionUpdateActor])
  private val END_STEP_ID = "-1"
  private val LOG_EVENT_STEP_ID = "-999"

  override def preStart():Unit = {
    LOGGER.info(s"Starting session update actor [${self.path.name}]")
    super.preStart()
  }

  override def postStop(): Unit = {
    LOGGER.info(s"Stopping session update actor [${self.path.name}]")
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
      if (step == END_STEP_ID) {
        try {
          var outputMessage: String = null
          testStepStatus.getReport match {
            case tar: TAR if tar.getContext != null && tar.getContext.getValue != null && !tar.getContext.getValue.isBlank =>
              outputMessage = tar.getContext.getValue.trim
            case _ => // Do nothing
          }
          reportManager.finishTestReport(session, testStepStatus.getReport.getResult, Option.apply(outputMessage))
          val statusUpdates: List[(String, TestStepResultInfo)] = testResultManager.sessionRemove(session)
          val resultInfo: TestStepResultInfo = new TestStepResultInfo(testStepStatus.getStatus.ordinal.toShort, Option.empty)
          val message: String = JsonUtil.jsTestStepResultInfo(session, step, resultInfo, Option.apply(outputMessage), statusUpdates).toString
          webSocketActor.testSessionEnded(session, message)
        } finally {
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
        interactWithUsersRequest.getInteraction.getInstructionOrRequest.asScala.foreach {
          case instruction: Instruction if !StringUtils.isBlank(instruction.getValue) && instruction.getEmbeddingMethod == ValueEmbeddingEnumeration.BASE_64 && StringUtils.isBlank(instruction.getName) => // Determine the file name from the BASE64 content.
            val mimeType = MimeUtil.getMimeType(instruction.getValue, false)
            val extension = MimeUtil.getExtensionFromMimeType(mimeType)
            if (extension != null) instruction.setName(s"file$extension")
          case _ => // Ignoring requests.
        }
      }
      if (WebSocketActor.webSockets.contains(session)) {
        val actor = interactWithUsersRequest.getInteraction.getWith
        val request = JacksonUtil.serializeInteractionRequest(interactWithUsersRequest)
        if (actor == null) { // if actor not specified, send the request to all actors. Let client side handle this.
          webSocketActor.broadcast(session, request)
        } else { //send the request only to the given actor
          webSocketActor.push(session, actor, request)
        }
      } else { // This is a headless session - automatically dismiss or complete with an empty request.
        LOGGER.warn(s"Headless session [$session] expected interaction for step [${interactWithUsersRequest.getStepId}]. Completed automatically with empty result.")
        val interactionResult = new ProvideInputRequest
        interactionResult.setTcInstanceId(session)
        interactionResult.setStepId(interactWithUsersRequest.getStepId)
        testbedBackendClient.service().provideInput(interactionResult)
      }
    } catch {
      case e: Exception =>
        LOGGER.error(s"Error during user interaction for session [$session]", e)
    }
  }

}
