package actors

import actors.events.sessions.{PrepareTestSessionsEvent, TerminateSessionsEvent, TestSessionConfiguredEvent}
import org.apache.pekko.actor.{Actor, ActorContext, ActorRef}
import com.gitb.tbs.{ConfigurationCompleteRequest, InteractWithUsersRequest, TestStepStatus}
import org.apache.commons.lang3.StringUtils
import org.apache.pekko.actor.Status.Failure
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.InjectedActorSupport
import utils.JacksonUtil

import java.util.UUID
import javax.inject.Inject

object SessionManagerActor {

  val actorName = "session-manager"

}

class SessionManagerActor @Inject() (sessionUpdateActorFactory: SessionUpdateActor.Factory, sessionLaunchActorFactory: SessionLaunchActor.Factory, webSocketActor: WebSocketActor) extends Actor with InjectedActorSupport {

  private val LOGGER = LoggerFactory.getLogger(classOf[SessionManagerActor])
  private val LAUNCH_ACTOR_PREFIX = "launch"

  override def preStart():Unit = {
    LOGGER.debug(s"Starting session manager actor [${self.path.name}]")
    super.preStart()
  }

  override def postStop(): Unit = {
    LOGGER.debug(s"Stopping session manager actor [${self.path.name}]")
    super.postStop()
  }

  override def receive: Receive = {
    case msg: TestStepStatus =>
      getSessionUpdateActor(msg.getTcInstanceId, context) ! msg
    case msg: InteractWithUsersRequest =>
      getSessionUpdateActor(msg.getTcInstanceid, context) ! msg
    case msg: ConfigurationCompleteRequest =>
      notifyConfigurationCompleteEvent(msg)
    case msg: PrepareTestSessionsEvent =>
      prepareTestSessions(msg)
    case msg: TerminateSessionsEvent =>
      terminateSessions(msg)
    case msg: Failure =>
      LOGGER.error("Session manager actor caught unexpected error", msg.cause)
    case msg: Object =>
      LOGGER.warn(s"Session manager received unexpected message [${msg.getClass.getName}]")
  }

  private def notifyConfigurationCompleteEvent(event: ConfigurationCompleteRequest): Unit = {
    try {
      // Notify open web sockets in case this is an interactive session
      webSocketActor.broadcast(event.getTcInstanceId, JacksonUtil.serializeConfigurationCompleteRequest(event))
      // Publish event in case this is a headless session
      context.system.eventStream.publish(TestSessionConfiguredEvent(event))
    } catch {
      case e: Exception =>
        LOGGER.error(s"Error during configuration of session [${event.getTcInstanceId}]", e)
    }
  }

  private def getSessionUpdateActor(sessionId: String, context: ActorContext): ActorRef = {
    context.child("session-"+sessionId) match {
      case Some(actorRef) => actorRef
      case None =>
        implicit val context: ActorContext = this.context
        injectedChild(sessionUpdateActorFactory(), "session-"+sessionId, props => props.withDispatcher("session-actor-dispatcher"))
    }
  }

  private def prepareTestSessions(event: PrepareTestSessionsEvent): Unit = {
    LOGGER.info("Starting batch of "+event.launchData.testCases.size+" test session(s)")
    implicit val context: ActorContext = this.context
    val actor = injectedChild(sessionLaunchActorFactory(), "%s-%s".formatted(LAUNCH_ACTOR_PREFIX, UUID.randomUUID().toString), props => props.withDispatcher("session-actor-dispatcher"))
    actor ! event
  }

  private def terminateSessions(event: TerminateSessionsEvent): Unit = {
    this.context.children.filter(x => StringUtils.startsWith(x.path.name, LAUNCH_ACTOR_PREFIX)).foreach { actor =>
      actor.tell(event, this.self)
    }
  }

}
