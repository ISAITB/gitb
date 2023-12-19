package actors

import actors.events.sessions.{PrepareTestSessionsEvent, TestSessionConfiguredEvent}
import org.apache.pekko.actor.{Actor, ActorContext, ActorRef}
import com.gitb.tbs.{ConfigurationCompleteRequest, InteractWithUsersRequest, TestStepStatus}
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
    case msg: Object =>
      LOGGER.warn(s"Session manager received unexpected message [${msg.getClass.getName}]")
  }

  private def notifyConfigurationCompleteEvent(event: ConfigurationCompleteRequest) = {
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
        LOGGER.info("Starting test session ["+sessionId+"]")
        implicit val context: ActorContext = this.context
        injectedChild(sessionUpdateActorFactory(), "session-"+sessionId, props => props.withDispatcher("session-actor-dispatcher"))
    }
  }

  private def prepareTestSessions(event: PrepareTestSessionsEvent): Unit = {
    LOGGER.info("Starting batch of "+event.launchData.testCases.size+" test session(s)")
    implicit val context: ActorContext = this.context
    val actor = injectedChild(sessionLaunchActorFactory(), "launch-"+UUID.randomUUID().toString, props => props.withDispatcher("session-actor-dispatcher"))
    actor ! event
  }

}
