/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package actors

import actors.SessionUpdateActor.{InteractWithUsersRequestWrapper, SessionCompleted, TaskCompleted, TestStepStatusWrapper, isSessionEndMessage, isSessionLogMessage, isSessionStopMessage}
import actors.events.sessions.TestSessionCompletedEvent
import com.gitb.tbs.{Instruction, InteractWithUsersRequest, TestStepStatus}
import com.gitb.tr.TAR
import managers.{ReportManager, TestExecutionManager, TestResultManager, TestbedBackendClient}
import models.{TestInteraction, TestStepResultInfo}
import org.apache.commons.lang3.StringUtils
import org.apache.pekko.actor.Status.Failure
import org.apache.pekko.actor.{Actor, PoisonPill}
import org.slf4j.LoggerFactory
import utils._

import java.nio.file.Files
import java.util.Objects
import javax.inject.Inject
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

object SessionUpdateActor {

  private val END_STEP_ID = "-1"
  private val END_STOP_STEP_ID = "-2"
  private val LOG_EVENT_STEP_ID = "-999"

  trait Factory {
    def apply(): Actor
  }

  def isSessionEndMessage(msg: TestStepStatus): Boolean = {
    msg.getStepId == END_STEP_ID
  }

  def isSessionStopMessage(msg: TestStepStatus): Boolean = {
    msg.getStepId == END_STOP_STEP_ID
  }

  def isSessionLogMessage(msg: TestStepStatus): Boolean = {
    msg.getStepId == LOG_EVENT_STEP_ID
  }

  private case class TestStepStatusWrapper(wrapped: TestStepStatus)
  private case class InteractWithUsersRequestWrapper(wrapped: InteractWithUsersRequest)
  private case class SessionCompleted(session: String)
  private case class TaskCompleted(message: Option[Any])

}

class SessionUpdateActor @Inject() (repositoryUtils: RepositoryUtils,
                                    testExecutionManager: TestExecutionManager,
                                    reportManager: ReportManager,
                                    testResultManager: TestResultManager,
                                    webSocketActor: WebSocketActor,
                                    testbedBackendClient: TestbedBackendClient)
                                   (implicit ec: ExecutionContext) extends Actor {

  private val LOGGER = LoggerFactory.getLogger(classOf[SessionUpdateActor])

  // We use a queue for any asynchronous work needed to ensure that all asynchronous tasks are processed in sequence.
  private val workQueue = mutable.Queue[() => Future[TaskCompleted]]()
  private var workQueueProcessing = false
  private var stopping = false

  override def preStart():Unit = {
    LOGGER.debug(s"Starting session update actor [${self.path.name}]")
    super.preStart()
  }

  override def postStop(): Unit = {
    LOGGER.debug(s"Stopping session update actor [${self.path.name}]")
    super.postStop()
  }

  override def receive: Receive = {
    /*
     * Wrap externally received messages and send to self, to ensure we process them in sequence.
     */
    case msg: TestStepStatus => self ! TestStepStatusWrapper(msg)
    case msg: InteractWithUsersRequest => self ! InteractWithUsersRequestWrapper(msg)
    /*
     * Handle events.
     */
    case msg: TestStepStatusWrapper =>
      if (!stopping) {
        updateStatus(msg.wrapped)
      }
    case msg: InteractWithUsersRequestWrapper =>
      if (!stopping) {
        interactWithUsers(msg.wrapped)
      }
    case msg: SessionCompleted =>
      if (!stopping) {
        handleSessionCompleted(msg)
      }
    case msg: TaskCompleted =>
      if (!stopping) {
        workQueueProcessing = false
        // If the task was bearing a message send it.
        msg.message.foreach(self ! _)
        // Continue processing the queue.
        processQueue()
      }
    case msg: Failure =>
      LOGGER.error("Session update actor caught unexpected error", msg.cause)
    case msg: Object =>
      LOGGER.warn(s"Session update actor received unexpected message [${msg.getClass.getName}]")
  }

  private def handleSessionCompleted(msg: SessionCompleted): Unit = {
    // Notify that a test session has completed.
    context.system.eventStream.publish(TestSessionCompletedEvent(msg.session))
    // Stop the current actor
    stopping = true
    self ! PoisonPill.getInstance
  }

  private def updateStatus(testStepStatus: TestStepStatus): Unit = {
    val session = testStepStatus.getTcInstanceId
    val step = testStepStatus.getStepId
    val job = () => {
      val task = if (isSessionEndMessage(testStepStatus) || isSessionStopMessage(testStepStatus)) {
        var outputMessage: String = null
        testStepStatus.getReport match {
          case tar: TAR if tar.getContext != null =>
            val outputMessages = new StringBuilder()
            if (!tar.getContext.getItem.isEmpty) {
              tar.getContext.getItem.stream()
                .filter(x => Objects.nonNull(x))
                .map(x => x.getValue)
                .filter(x => StringUtils.isNotBlank(x))
                .map(x => x.trim)
                .forEach(x => outputMessages.append('\n').append(x))
              outputMessage = outputMessages.toString().trim
            } else if (StringUtils.isNotBlank(tar.getContext.getValue)) {
              outputMessage = tar.getContext.getValue.trim
            }
          case _ => // Do nothing
        }
        // Save report
        testExecutionManager.finishTestReport(session, testStepStatus.getReport.getResult, Option(outputMessage)).map { _ =>
          val statusUpdates: List[(String, TestStepResultInfo)] = testResultManager.sessionRemove(session)
          val resultInfo: TestStepResultInfo = new TestStepResultInfo(testStepStatus.getStatus.ordinal.toShort, None)
          val message: String = JsonUtil.jsTestStepResultInfo(session, step, resultInfo, Option(outputMessage), statusUpdates).toString
          webSocketActor.testSessionEnded(session, message)
          TaskCompleted(Some(SessionCompleted(session)))
        }.recover {
          case e: Exception =>
            LOGGER.error(s"Error during test session finalisation for session [{}]", session, e)
            TaskCompleted(Some(SessionCompleted(session)))
        }
      } else {
        if (isSessionLogMessage(testStepStatus)) { //send log event
          Future.successful {
            testStepStatus.getReport match {
              case tar: TAR if tar.getContext != null =>
                val logMessage: String = tar.getContext.getValue
                testResultManager.sessionUpdate(session, logMessage)
              case _ =>
            }
            webSocketActor.broadcast(session, JacksonUtil.serializeTestStepStatus(testStepStatus), retry = false)
            TaskCompleted(None)
          }
        } else {
          reportManager.createTestStepReport(session, testStepStatus).map { reportPath =>
            val resultInfo: TestStepResultInfo = new TestStepResultInfo(testStepStatus.getStatus.ordinal.toShort, reportPath)
            val statusUpdates: List[(String, TestStepResultInfo)] = testResultManager.sessionUpdate(session, step, resultInfo)
            val message: String = JsonUtil.jsTestStepResultInfo(session, step, resultInfo, Option.empty, statusUpdates).toString
            webSocketActor.broadcast(session, message)
            TaskCompleted(None)
          }
        }
      }
      task.recover {
        case e: Exception =>
          LOGGER.error(s"Error during test session update for session [{}]", testStepStatus.getTcInstanceId, e)
          TaskCompleted(None)
      }
    }
    queueTask(job)
  }

  private def interactWithUsers(interactWithUsersRequest: InteractWithUsersRequest): Unit = {
    val session = interactWithUsersRequest.getTcInstanceid
    val job = () => {
      val task = if (interactWithUsersRequest.getInteraction != null) {
        repositoryUtils.getPathForTestSession(interactWithUsersRequest.getTcInstanceid, isExpected = false).flatMap { sessionFolder =>
          val sessionFolderPath = sessionFolder.path
          Files.createDirectories(sessionFolderPath)
          interactWithUsersRequest.getInteraction.getInstructionOrRequest.asScala.foreach {
            case instruction: Instruction if !StringUtils.isBlank(instruction.getValue) && StringUtils.isBlank(instruction.getName) => // Determine the file name from the BASE64 content.
              val mimeType = MimeUtil.getMimeType(instruction.getValue, false)
              val extension = MimeUtil.getExtensionFromMimeType(mimeType)
              // Determine name.
              if (extension != null) {
                instruction.setName(s"file$extension")
              }
              // Decouple large content if needed into file references.
              repositoryUtils.decoupleLargeData(instruction, sessionFolderPath, isTempData = true)
            case _ => // Ignoring requests.
          }
          val request = JacksonUtil.serializeInteractionRequest(interactWithUsersRequest)
          testResultManager.saveTestInteraction(TestInteraction(interactWithUsersRequest.getTcInstanceid, interactWithUsersRequest.getStepId, interactWithUsersRequest.getInteraction.isAdmin, TimeUtil.getCurrentTimestamp(), request)).flatMap { _ =>
            if (WebSocketActor.webSockets.contains(session)) {
              Future.successful {
                val actor = interactWithUsersRequest.getInteraction.getWith
                if (actor == null) { // if actor not specified, send the request to all actors. Let client side handle this.
                  webSocketActor.broadcast(session, request)
                } else { //send the request only to the given actor
                  webSocketActor.push(session, actor, request)
                }
                TaskCompleted(None)
              }
            } else {
              // This is a headless session.
              if (!interactWithUsersRequest.getInteraction.isAdmin && !interactWithUsersRequest.getInteraction.isHasTimeout) {
                // This is a non-admin interaction for which we don't have a timeout defined - resolve the step immediately with empty inputs.
                // Admin interactions are not force-completed like this because they will normally always be done asynchronously.
                // They remain active indefinitely or until a configured timeout expires (handled via gitb-srv).
                LOGGER.warn(s"Headless session [$session] expected interaction for step [${interactWithUsersRequest.getStepId}]. Completed automatically with empty result.")
                testbedBackendClient.provideInput(session, interactWithUsersRequest.getStepId, None, interactWithUsersRequest.getInteraction.isAdmin).map { _ =>
                  TaskCompleted(None)
                }
              } else {
                Future.successful {
                  TaskCompleted(None)
                }
              }
            }
          }
        }
      } else {
        LOGGER.warn(s"Session [$session] received empty interaction for step [${interactWithUsersRequest.getStepId}].")
        testbedBackendClient.provideInput(session, interactWithUsersRequest.getStepId, None, interactWithUsersRequest.getInteraction.isAdmin).map { _ =>
          TaskCompleted(None)
        }
      }
      task.recover {
        case e: Exception =>
          LOGGER.error(s"Error during user interaction for session [$session]", e)
          TaskCompleted(None)
      }
    }
    queueTask(job)
  }

  private def queueTask(task: () => Future[TaskCompleted]): Unit = {
    workQueue.enqueue(task)
    processQueue()
  }

  private def processQueue(): Unit = {
    if (!workQueueProcessing && workQueue.nonEmpty) {
      workQueueProcessing = true
      val task = workQueue.dequeue()
      task().foreach(self ! _)  // When the Future completes, send message to self
    }
  }

}
