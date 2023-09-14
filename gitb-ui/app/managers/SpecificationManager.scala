package managers

import models.{Actors, Constants, Endpoints, SpecificationGroups, Specifications}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.CryptoUtil

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SpecificationManager @Inject() (testResultManager: TestResultManager, conformanceManager: ConformanceManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getSpecificationGroups(domainId: Long): List[SpecificationGroups] = {
    getSpecificationGroupsByDomainIds(Some(List(domainId)))
  }

  def getSpecificationGroupsByDomainIds(domainIds: Option[List[Long]]): List[SpecificationGroups] = {
    exec (
      PersistenceSchema.specificationGroups
        .filterOpt(domainIds)((q, ids) => q.domain inSet ids)
        .sortBy(_.shortname.asc)
        .result
    ).toList
  }

  def getSpecificationGroupById(groupId: Long): SpecificationGroups = {
    exec(PersistenceSchema.specificationGroups.filter(_.id === groupId).result.head)
  }

  def createSpecificationGroup(group: SpecificationGroups): Long = {
    exec(createSpecificationGroupInternal(group).transactionally)
  }

  def createSpecificationGroupInternal(group: SpecificationGroups):DBIO[Long] = {
    PersistenceSchema.specificationGroups.returning(PersistenceSchema.specificationGroups.map(_.id)) += group
  }

  def updateSpecificationGroup(groupId: Long, shortname: String, fullname: String, description: Option[String]): Unit = {
    exec(updateSpecificationGroupInternal(groupId, shortname, fullname, description, None).transactionally)
  }

  def updateSpecificationGroupInternal(groupId: Long, shortname: String, fullname: String, description: Option[String], displayOrder: Option[Short]): DBIO[_] = {
    for {
      _ <- PersistenceSchema.specificationGroups
        .filter(_.id === groupId)
        .map(x => (x.shortname, x.fullname, x.description))
        .update(shortname, fullname, description)
      _ <- {
        if (displayOrder.isDefined) {
          PersistenceSchema.specificationGroups.filter(_.id === groupId).map(_.displayOrder).update(displayOrder.get)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  def removeSpecificationFromGroup(specificationId: Long): Unit = {
    exec(PersistenceSchema.specifications.filter(_.id === specificationId).map(x => (x.group, x.displayOrder)).update(None, 0).transactionally)
  }

  def addSpecificationToGroup(specificationId: Long, groupId: Long): Unit = {
    exec(PersistenceSchema.specifications.filter(_.id === specificationId).map(x => (x.group, x.displayOrder)).update(Some(groupId), 0).transactionally)
  }

  def copySpecificationToGroup(specificationId: Long, groupId: Long): Long = {
    exec((
      for {
        // Load existing specification.
        spec <- PersistenceSchema.specifications.filter(_.id === specificationId).result.head
        // Create copy.
        newSpecId <- PersistenceSchema.insertSpecification += Specifications(
          0L, spec.shortname, spec.fullname, spec.description,
          spec.hidden, CryptoUtil.generateApiKey(), spec.domain, 0, Some(groupId)
        )
        // Copy actors, endpoints and parameters.
        actors <- PersistenceSchema.actors
          .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
          .filter(_._2.specId === specificationId)
          .map(_._1)
          .result
        _ <- {
          val actions = ListBuffer[DBIO[_]]()
          actors.foreach { actor =>
            actions += copyActor(actor, newSpecId)
          }
          toDBIO(actions)
        }
      } yield newSpecId
    ).transactionally)
  }

  private def copyActor(actor: Actors, specificationId: Long): DBIO[_] = {
    val oldActorId = actor.id
    for {
      // Actor.
      newActorId <- PersistenceSchema.insertActor += Actors(
        0L, actor.actorId, actor.name, actor.description,
        actor.default, actor.hidden, actor.displayOrder, CryptoUtil.generateApiKey(), actor.domain
      )
      // Specification link.
      _ <- PersistenceSchema.specificationHasActors += (specificationId, newActorId)
      // Endpoints.
      endpoints <- PersistenceSchema.endpoints.filter(_.actor === oldActorId).result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        endpoints.foreach { endpoint =>
          actions += copyEndpoint(endpoint, newActorId)
        }
        toDBIO(actions)
      }
    } yield ()
  }

  private def copyEndpoint(endpoint: Endpoints, actorId: Long): DBIO[_] = {
    val oldEndpointId = endpoint.id
    for {
      // Endpoint.
      newEndpointId <- PersistenceSchema.insertEndpoint += Endpoints(
        0L, endpoint.name, endpoint.desc, actorId
      )
      // Parameters.
      parameters <- PersistenceSchema.parameters.filter(_.endpoint === oldEndpointId).result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        parameters.foreach { parameter =>
          actions += (PersistenceSchema.insertParameter += parameter.withEndpoint(newEndpointId, None))
        }
        toDBIO(actions)
      }
    } yield ()
  }

  def deleteSpecificationGroup(groupId: Long, deleteSpecifications: Boolean):Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteSpecificationGroupInternal(groupId, deleteSpecifications, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteSpecificationGroupInternal(groupId: Long, deleteSpecifications: Boolean, onSuccessCalls: mutable.ListBuffer[() => _]):DBIO[_] = {
    val dbAction = for {
      // See if we need to delete specifications as well.
      specificationsToDelete <- {
        if (deleteSpecifications) {
          PersistenceSchema.specifications.filter(_.group === groupId).map(_.id).result
        } else {
          DBIO.successful(List.empty)
        }
      }
      // Delete the selected specifications.
      _ <- {
        if (specificationsToDelete.nonEmpty) {
          val actions = ListBuffer[DBIO[_]]()
          specificationsToDelete.foreach { specification =>
            actions += conformanceManager.deleteSpecificationInternal(specification, onSuccessCalls)
          }
          toDBIO(actions)
        } else {
          DBIO.successful(())
        }
      }
      // Delete the group.
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        if (!deleteSpecifications) {
          // There may be linked specifications to the group.
          actions += PersistenceSchema.specifications.filter(_.group === groupId).map(x => (x.group, x.displayOrder)).update(None, 0)
        }
        actions += PersistenceSchema.conformanceSnapshotResults.filter(_.specificationGroupId === groupId).map(_.specificationGroupId).update(Some(groupId * -1))
        actions += PersistenceSchema.specificationGroups.filter(_.id === groupId).delete
        toDBIO(actions)
      }
    } yield ()
    dbAction
  }

  /**
   * Checks if domain exists
   */
  def checkSpecificationExists(specId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.specifications.filter(_.id === specId).result.headOption)
    firstOption.isDefined
  }

  def getSpecificationById(specId: Long): Specifications = {
    val spec = exec(PersistenceSchema.specifications.filter(_.id === specId).result.head)
    spec
  }

  def getSpecificationsById(specIds: List[Long]): List[Specifications] = {
    exec(PersistenceSchema.specifications.filter(_.id inSet specIds).result.map(_.toList))
  }

  def getSpecificationInfoByApiKeys(specificationApiKey: Option[String], communityApiKey: String): Option[(Long, Option[Long])] = { // Domain ID and specification ID
    exec(for {
      communityIds <- {
        PersistenceSchema.communities.filter(_.apiKey === communityApiKey).map(x => (x.id, x.domain)).result.headOption
      }
      relevantDomainId <- {
        if (communityIds.isDefined) {
          var domainId: Option[Long] = None
          if (communityIds.get._1 != Constants.DefaultCommunityId) {
            domainId = communityIds.get._2
          }
          DBIO.successful(domainId)
        } else {
          DBIO.successful(None)
        }
      }
      specificationIds <- {
        if (specificationApiKey.isDefined) {
          PersistenceSchema.specifications
            .filter(_.apiKey === specificationApiKey.get)
            .filterOpt(relevantDomainId)((q, id) => q.domain === id)
            .map(x => (x.domain, x.id))
            .result.headOption
        } else {
          DBIO.successful(None)
        }
      }
      idsToReturn <- {
        if (specificationIds.isDefined) {
          DBIO.successful(Some(specificationIds.get._1, Some(specificationIds.get._2)))
        } else if (relevantDomainId.isDefined) {
          DBIO.successful(Some(relevantDomainId.get, None))
        } else {
          DBIO.successful(None)
        }
      }
    } yield idsToReturn)
  }

  def updateSpecificationInternal(specId: Long, sname: String, fname: String, descr: Option[String], hidden:Boolean, apiKey: Option[String], checkApiKeyUniqueness: Boolean, groupId: Option[Long], displayOrder: Option[Short]): DBIO[_] = {
    for {
      _ <- {
        val q = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.shortname, s.fullname, s.description, s.hidden, s.group)
        q.update(sname, fname, descr, hidden, groupId) andThen
          testResultManager.updateForUpdatedSpecification(specId, sname)
      }
      _ <- {
        if (displayOrder.isDefined) {
          PersistenceSchema.specifications.filter(_.id === specId).map(_.displayOrder).update(displayOrder.get)
        } else {
          DBIO.successful(())
        }
      }
      replaceApiKey <- {
        if (apiKey.isDefined && checkApiKeyUniqueness) {
          PersistenceSchema.specifications.filter(_.apiKey === apiKey.get).filter(_.id =!= specId).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      _ <- {
        if (apiKey.isDefined) {
          val apiKeyToUse = if (replaceApiKey) CryptoUtil.generateApiKey() else apiKey.get
          PersistenceSchema.specifications.filter(_.id === specId).map(_.apiKey).update(apiKeyToUse)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  def updateSpecification(specId: Long, sname: String, fname: String, descr: Option[String], hidden:Boolean, groupId: Option[Long]) = {
    exec(updateSpecificationInternal(specId, sname, fname, descr, hidden, None, checkApiKeyUniqueness = false, groupId, None).transactionally)
  }

  def getSpecificationIdOfActor(actorId: Long) = {
    exec(PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).result.head)._1
  }

  def getSpecificationOfActor(actorId: Long) = {
    exec(getSpecificationOfActorInternal(actorId))
  }

  def getSpecificationOfActorInternal(actorId: Long): DBIO[Specifications] = {
    PersistenceSchema.specifications
      .join(PersistenceSchema.specificationHasActors).on(_.id === _.specId)
      .filter(_._2.actorId === actorId)
      .map(_._1)
      .result
      .head
  }

  def saveSpecificationOrder(groupIds: List[Long], groupOrders: List[Long], specIds: List[Long], specOrders: List[Long]): Unit = {
    val dbActions = new ListBuffer[DBIO[_]]
    for (i <- groupIds.indices) {
      dbActions += PersistenceSchema.specificationGroups.filter(_.id === groupIds(i)).map(_.displayOrder).update(groupOrders(i).toShort)
    }
    for (i <- specIds.indices) {
      dbActions += PersistenceSchema.specifications.filter(_.id === specIds(i)).map(_.displayOrder).update(specOrders(i).toShort)
    }
    exec(toDBIO(dbActions))
  }

  def resetSpecificationOrder(domainId: Long): Unit = {
    exec(
      PersistenceSchema.specifications.filter(_.domain === domainId).map(_.displayOrder).update(0) andThen
      PersistenceSchema.specificationGroups.filter(_.domain === domainId).map(_.displayOrder).update(0)
    )
  }
}
