package managers

import actors.events.{ConformanceStatementCreatedEvent, OrganisationCreatedEvent, SystemCreatedEvent, TriggerEvent}
import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import models.{OrganisationCreationDbInfo, SystemCreationDbInfo}

@Singleton
class TriggerHelper @Inject() (actorSystem: ActorSystem) {

  def publishTriggerEvent[T <: TriggerEvent](event:T):Unit = {
    actorSystem.eventStream.publish(event)
  }

  def triggersFor(communityId: Long, organisationInfo: OrganisationCreationDbInfo):Unit = {
    publishTriggerEvent(new OrganisationCreatedEvent(communityId, organisationInfo.organisationId))
    triggersFor(communityId, organisationInfo.systems)
  }

  def triggersFor(communityId: Long, systemsInfo: Option[List[SystemCreationDbInfo]]):Unit = {
    if (systemsInfo.isDefined) {
      systemsInfo.get.foreach { systemInfo =>
        triggersFor(communityId, systemInfo)
      }
    }
  }

  def triggersFor(communityId: Long, systemInfo: SystemCreationDbInfo):Unit = {
    publishTriggerEvent(new SystemCreatedEvent(communityId, systemInfo.systemId))
    triggersFor(communityId, systemInfo.systemId, systemInfo.linkedActorIds)
  }

  def triggersFor(communityId: Long, systemId: Long, linkedActorIds: Option[List[Long]]):Unit = {
    if (linkedActorIds.isDefined) {
      linkedActorIds.get.foreach { actorId =>
        publishTriggerEvent(new ConformanceStatementCreatedEvent(communityId, systemId, actorId))
      }
    }
  }

}
