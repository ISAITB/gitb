package managers

import actors.events.{ConformanceStatementCreatedEvent, OrganisationCreatedEvent, SystemCreatedEvent, TriggerEvent}
import models.Enums.TriggerDataType
import models.Enums.TriggerDataType.TriggerDataType
import models.{OrganisationCreationDbInfo, SystemCreationDbInfo}
import org.apache.pekko.actor.ActorSystem
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TriggerHelper @Inject() (actorSystem: ActorSystem,
                               dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

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

  def deleteTrigger(triggerId: Long): Unit = {
    exec(deleteTriggerInternal(triggerId).transactionally)
  }

  private[managers] def deleteTriggersByCommunity(communityId: Long): DBIO[_] = {
    for {
      triggerIds <- PersistenceSchema.triggers.filter(_.community === communityId).map(x => x.id).result
      _ <- {
        val dbActions = ListBuffer[DBIO[_]]()
        triggerIds.foreach { triggerId =>
          dbActions += deleteTriggerInternal(triggerId)
        }
        toDBIO(dbActions)
      }
    } yield ()
  }

  private[managers] def deleteTriggerInternal(triggerId: Long): DBIO[_] = {
    deleteTriggerDataInternal(triggerId) andThen
      deleteTriggerFireExpressionsInternal(triggerId) andThen
      PersistenceSchema.triggers.filter(_.id === triggerId).delete
  }

  private[managers] def deleteTriggerDataByDataType(dataId: Long, dataType: TriggerDataType): DBIO[_] = {
    PersistenceSchema.triggerData.filter(_.dataId === dataId).filter(_.dataType === dataType.id.toShort).delete
  }

  private[managers] def deleteTriggerDataInternal(triggerId: Long): DBIO[_] = {
    PersistenceSchema.triggerData.filter(_.trigger === triggerId).delete
  }

  private[managers] def deleteTriggerFireExpressionsInternal(triggerId: Long): DBIO[_] = {
    PersistenceSchema.triggerFireExpressions.filter(_.trigger === triggerId).delete
  }

  def deleteTriggerDataOfCommunityAndDomain(communityId: Long, domainId: Long): DBIO[_] = {
    for {
      domainParameterIds <- {
        PersistenceSchema.domainParameters.filter(_.domain === domainId).map(x => x.id).result
      }
      triggerDataToDelete <- {
        PersistenceSchema.triggerData
          .join(PersistenceSchema.triggers).on(_.trigger === _.id)
          .filter(_._2.community === communityId)
          .filter(_._1.dataType === TriggerDataType.DomainParameter.id.toShort)
          .filter(_._1.dataId inSet domainParameterIds)
          .map(x => x._1)
          .result
      }
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        triggerDataToDelete.foreach { data =>
          actions += PersistenceSchema.triggerData
            .filter(_.dataId === data.dataId)
            .filter(_.dataType === data.dataType)
            .filter(_.trigger === data.trigger)
            .delete
        }
        toDBIO(actions)
      }
    } yield ()
  }

}
