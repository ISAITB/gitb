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

import actors.events.TriggerEvent
import managers.TriggerManager
import org.apache.pekko.actor._
import org.slf4j.LoggerFactory

import javax.inject.Inject

object TriggerActor {
  val actorName = "trigger-actor"
}

class TriggerActor @Inject() (triggerManager: TriggerManager) extends Actor {

  private def logger = LoggerFactory.getLogger(classOf[TriggerActor])

  override def preStart():Unit = {
    logger.info(s"Starting trigger actor [${self.path.name}]")
    super.preStart()
    context.system.eventStream.subscribe(context.self, classOf[TriggerEvent])
  }

  override def postStop(): Unit = {
    logger.info(s"Stopping trigger actor [${self.path.name}]")
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
      triggerManager.fireTriggers(msg.communityId, msg.eventType, msg.sessionId)
    case msg: actors.events.TestSessionFailedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, msg.sessionId)
    case msg: actors.events.TestSessionStartedEvent =>
      triggerManager.fireTriggers(msg.communityId, msg.eventType, msg.sessionId)
    case msg: actors.events.TriggerEvent =>
      logger.warn("Unexpected event type received [community: "+msg.communityId+"][type: "+msg.eventType+"]")
  }

}
