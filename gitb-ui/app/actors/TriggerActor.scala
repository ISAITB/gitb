package actors

import actors.events.TriggerEvent
import akka.actor._
import javax.inject.Inject
import managers.TriggerManager
import org.slf4j.LoggerFactory

object TriggerActor {
  val actorName = "trigger-actor"
}

class TriggerActor @Inject() (triggerManager: TriggerManager) extends Actor {

  private def logger = LoggerFactory.getLogger(classOf[TriggerActor])

  override def preStart:Unit = {
    logger.info("Starting trigger actor")
    super.preStart()
    context.system.eventStream.subscribe(context.self, classOf[TriggerEvent])
  }

  override def postStop(): Unit = {
    logger.info("Stopping trigger actor")
    super.postStop()
    context.system.eventStream.unsubscribe(context.self)
  }

  override def receive: Receive = {
    case msg: actors.events.OrganisationCreatedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, msg.organisationId)
    case msg: actors.events.OrganisationUpdatedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, msg.organisationId)
    case msg: actors.events.SystemCreatedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, msg.systemId)
    case msg: actors.events.SystemUpdatedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, msg.systemId)
    case msg: actors.events.ConformanceStatementCreatedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, (msg.systemId, msg.actorId))
    case msg: actors.events.ConformanceStatementUpdatedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, (msg.systemId, msg.actorId))
    case msg: actors.events.ConformanceStatementSucceededEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, (msg.systemId, msg.actorId))
    case msg: actors.events.TestSessionSucceededEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, (msg.systemId, msg.actorId))
    case msg: actors.events.TestSessionFailedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, (msg.systemId, msg.actorId))
    case msg: actors.events.TriggerEvent =>
      logger.warn("Unexpected event type received [community: "+msg.communityId+"][type: "+msg.eventType+"]")
  }

}
