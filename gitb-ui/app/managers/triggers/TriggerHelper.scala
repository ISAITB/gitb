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

package managers.triggers

import actors.events.{ConformanceStatementCreatedEvent, OrganisationCreatedEvent, SystemCreatedEvent, TriggerEvent}
import managers.BaseManager
import models.Enums.TriggerDataType
import models.Enums.TriggerDataType.TriggerDataType
import models.{OrganisationCreationDbInfo, SystemCreationDbInfo}
import org.apache.pekko.actor.ActorSystem
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import javax.inject.{Inject, Singleton}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TriggerHelper @Inject() (actorSystem: ActorSystem,
                               dbConfigProvider: DatabaseConfigProvider)
                              (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

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

  def deleteTrigger(triggerId: Long): Future[Unit] = {
    DB.run(deleteTriggerInternal(triggerId).transactionally)
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

  private[managers] def deleteTriggerInternal(triggerId: Long): DBIO[Unit] = {
    for {
      _ <- deleteTriggerDataInternal(triggerId)
      _ <- deleteTriggerFireExpressionsInternal(triggerId)
      _ <- PersistenceSchema.triggers.filter(_.id === triggerId).delete
    } yield ()
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
