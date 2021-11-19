package actors

import akka.actor.{Actor, ActorContext, ActorRef}
import com.gitb.tbs.{InteractWithUsersRequest, TestStepStatus}
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.InjectedActorSupport

import javax.inject.Inject

object SessionManagerActor {

  val actorName = "session-manager"

}

class SessionManagerActor @Inject() (childFactory: SessionUpdateActor.Factory) extends Actor with InjectedActorSupport {

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
    case msg: Object =>
      LOGGER.warn(s"Session manager received unexpected message [${msg.getClass.getName}]")
  }

  private def getSessionUpdateActor(sessionId: String, context: ActorContext): ActorRef = {
    context.child(sessionId) match {
      case Some(actorRef) => actorRef
      case None =>
        implicit val context: ActorContext = this.context
        injectedChild(childFactory(), sessionId, props => props.withDispatcher("session-actor-dispatcher"))
    }
  }

}
