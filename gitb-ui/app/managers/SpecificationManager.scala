package managers

import models.Enums.TestResultStatus
import models._
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, RepositoryUtils}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SpecificationManager @Inject() (repositoryUtils: RepositoryUtils, testSuiteManager: TestSuiteManager, actorManager: ActorManager, testResultManager: TestResultManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def deleteSpecificationByDomain(domainId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val action = (for {
      ids <- PersistenceSchema.specifications.filter(_.domain === domainId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => deleteSpecificationInternal(id, onSuccessCalls)): _*)
    } yield ()).transactionally
    action
  }

  def deleteSpecification(specId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = deleteSpecificationInternal(specId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def deleteSpecificationInternal(specId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      _ <- testResultManager.updateForDeletedSpecification(specId)
      // Delete also actors from the domain (they are now linked only to specifications
      actorIds <- PersistenceSchema.specificationHasActors.filter(_.specId === specId).map(_.actorId).result
      _ <- DBIO.seq(actorIds.map(id => actorManager.deleteActor(id, onSuccessCalls)): _*)
      _ <- PersistenceSchema.specificationHasActors.filter(_.specId === specId).delete
      testSuiteIds <- {
        PersistenceSchema.specificationHasTestSuites
          .join(PersistenceSchema.testSuites).on(_.testSuiteId === _.id)
          .filter(_._1.specId === specId)
          .filter(_._2.shared =!= true) // We must keep shared test suites.
          .map(_._2.id)
          .result
      }
      _ <- DBIO.seq(testSuiteIds.map(id => testSuiteManager.undeployTestSuite(id, onSuccessCalls)): _*)
      _ <- PersistenceSchema.specificationHasTestSuites.filter(_.specId === specId).delete
      _ <- PersistenceSchema.conformanceOverviewCertificateMessages.filter(row => row.specification.isDefined && row.specification === specId).delete
      _ <- PersistenceSchema.conformanceSnapshotResults.filter(_.specificationId === specId).map(_.specificationId).update(specId * -1)
      _ <- PersistenceSchema.conformanceSnapshotSpecifications.filter(_.id === specId).map(_.id).update(specId * -1)
      _ <- PersistenceSchema.conformanceSnapshotOverviewCertificateMessages.filter(row => row.specificationId.isDefined && row.specificationId === specId).map(_.specificationId).update(Some(specId * -1))
      _ <- PersistenceSchema.conformanceResults.filter(_.spec === specId).delete
      _ <- PersistenceSchema.specifications.filter(_.id === specId).delete
      _ <- {
        onSuccessCalls += (() => repositoryUtils.deleteSpecificationBadges(specId))
        DBIO.successful(())
      }
    } yield ()
  }

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
    exec(createSpecificationGroupInternal(group, checkApiKeyUniqueness = false).transactionally)
  }

  def createSpecificationGroupInternal(group: SpecificationGroups, checkApiKeyUniqueness: Boolean):DBIO[Long] = {
    for {
      replaceApiKey <- if (checkApiKeyUniqueness) {
        PersistenceSchema.specificationGroups.filter(_.apiKey === group.apiKey).exists.result
      } else {
        DBIO.successful(false)
      }
      newGroupId <- {
        val groupToUse = if (replaceApiKey) group.withApiKey(CryptoUtil.generateApiKey()) else group
        PersistenceSchema.specificationGroups.returning(PersistenceSchema.specificationGroups.map(_.id)) += groupToUse
      }
    } yield newGroupId
  }

  def updateSpecificationGroup(groupId: Long, shortname: String, fullname: String, description: Option[String]): Unit = {
    exec(updateSpecificationGroupInternal(groupId, shortname, fullname, description, None, None, checkApiKeyUniqueness = false).transactionally)
  }

  def updateSpecificationGroupInternal(groupId: Long, shortname: String, fullname: String, description: Option[String], displayOrder: Option[Short], apiKey: Option[String], checkApiKeyUniqueness: Boolean): DBIO[_] = {
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
      replaceApiKey <- {
        if (apiKey.isDefined && checkApiKeyUniqueness) {
          PersistenceSchema.specificationGroups.filter(_.apiKey === apiKey.get).filter(_.id =!= groupId).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      _ <- {
        if (apiKey.isDefined) {
          val apiKeyToUse = if (replaceApiKey) CryptoUtil.generateApiKey() else apiKey.get
          PersistenceSchema.specificationGroups.filter(_.id === groupId).map(_.apiKey).update(apiKeyToUse)
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
            actions += deleteSpecificationInternal(specification, onSuccessCalls)
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
        actions += PersistenceSchema.conformanceOverviewCertificateMessages.filter(row => row.group.isDefined && row.group === groupId).delete
        actions += PersistenceSchema.conformanceSnapshotResults.filter(_.specificationGroupId === groupId).map(_.specificationGroupId).update(Some(groupId * -1))
        actions += PersistenceSchema.conformanceSnapshotSpecificationGroups.filter(_.id === groupId).map(_.id).update(groupId * -1)
        actions += PersistenceSchema.conformanceSnapshotOverviewCertificateMessages.filter(row => row.groupId.isDefined && row.groupId === groupId).map(_.groupId).update(Some(groupId * -1))
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
    getSpecificationsById(List(specId)).head
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

  def updateSpecificationInternal(specId: Long, sname: String, fname: String, descr: Option[String], hidden:Boolean, apiKey: Option[String], checkApiKeyUniqueness: Boolean, groupId: Option[Long], displayOrder: Option[Short], badges: BadgeInfo, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
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
      _ <- {
        onSuccessCalls += (() => updateSpecificationBadges(specId, badges.forWeb, badges.forReport))
        DBIO.successful(())
      }
    } yield ()
  }

  private def updateSpecificationBadges(specId: Long, badges: Badges, badgesForReport: Badges): Unit = {
    // We can either have no badges or at least the success and other ones.
    if ((!badges.hasSuccess && !badges.hasOther && !badges.hasFailure) || (badges.hasSuccess && badges.hasOther)) {
      updateSpecificationBadgesForCase(specId, badges, forReport = false)
      updateSpecificationBadgesForCase(specId, badgesForReport, forReport = true)
    }
  }

  private def updateSpecificationBadgesForCase(specId: Long, badges: Badges, forReport: Boolean): Unit = {
    // Delete previous or removed files.
    if (!badges.hasSuccess || badges.success.isDefined) {
      repositoryUtils.deleteSpecificationBadge(specId, TestResultStatus.SUCCESS.toString, forReport)
    }
    if (!badges.hasOther || badges.other.isDefined) {
      repositoryUtils.deleteSpecificationBadge(specId, TestResultStatus.UNDEFINED.toString, forReport)
    }
    if (!badges.hasFailure || badges.failure.isDefined) {
      repositoryUtils.deleteSpecificationBadge(specId, TestResultStatus.FAILURE.toString, forReport)
    }
    // Add new files.
    if (badges.success.isDefined) repositoryUtils.setSpecificationBadge(specId, badges.success.get, TestResultStatus.SUCCESS.toString, forReport)
    if (badges.other.isDefined) repositoryUtils.setSpecificationBadge(specId, badges.other.get, TestResultStatus.UNDEFINED.toString, forReport)
    if (badges.failure.isDefined) repositoryUtils.setSpecificationBadge(specId, badges.failure.get, TestResultStatus.FAILURE.toString, forReport)
  }

  def updateSpecification(specId: Long, sname: String, fname: String, descr: Option[String], hidden:Boolean, groupId: Option[Long], badges: BadgeInfo): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateSpecificationInternal(specId, sname, fname, descr, hidden, None, checkApiKeyUniqueness = false, groupId, None, badges, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def getSpecificationIdOfActor(actorId: Long): Long = {
    exec(PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).result.head)._1
  }

  def getSpecificationOfActor(actorId: Long): Specifications = {
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

  private def mergeSpecsWithGroups(data: Seq[(Specifications, Option[SpecificationGroups])]): Seq[Specifications] = {
    data.map { specData =>
      if (specData._2.isEmpty) {
        specData._1
      } else {
        specData._1.withGroupNames(specData._2.get.shortname, specData._2.get.fullname)
      }
    }.sortBy(_.shortname)
  }

  def getSpecifications(ids: Option[Iterable[Long]] = None, domainIds: Option[List[Long]] = None, groupIds: Option[List[Long]] = None, withGroups: Boolean = false): Seq[Specifications] = {
    val specs = if (withGroups) {
      val specData = exec(
        PersistenceSchema.specifications
          .joinLeft(PersistenceSchema.specificationGroups).on(_.group === _.id)
          .filterOpt(ids)((q, ids) => q._1.id inSet ids)
          .filterOpt(domainIds)((q, domainIds) => q._1.domain inSet domainIds)
          .filterOpt(groupIds)((q, groupIds) => q._1.group inSet groupIds)
          .map(x => (x._1, x._2))
          .result
      )
      mergeSpecsWithGroups(specData)
    } else {
      exec(
        PersistenceSchema.specifications
          .filterOpt(ids)((q, ids) => q.id inSet ids)
          .filterOpt(domainIds)((q, domainIds) => q.domain inSet domainIds)
          .filterOpt(groupIds)((q, groupIds) => q.group inSet groupIds)
          .sortBy(_.shortname.asc)
          .result
          .map(_.toList)
      )
    }
    specs
  }

  def getSpecifications(domain: Long, withGroups: Boolean): Seq[Specifications] = {
    val specs = if (withGroups) {
      val specData = exec(
        PersistenceSchema.specifications
          .joinLeft(PersistenceSchema.specificationGroups).on(_.group === _.id)
          .filter(_._1.domain === domain)
          .map(x => (x._1, x._2))
          .result
      )
      mergeSpecsWithGroups(specData)
    } else {
      exec(
        PersistenceSchema.specifications
          .filter(_.domain === domain)
          .sortBy(_.shortname.asc)
          .result
      )
    }
    specs
  }

  def getSpecificationsLinkedToTestSuite(testSuiteId: Long): Seq[Specifications] = {
    val specificationIds = exec(
      PersistenceSchema.specificationHasTestSuites.filter(_.testSuiteId === testSuiteId).map(_.specId).result
    )
    getSpecifications(Some(specificationIds), None, withGroups = true)
  }

  def createSpecificationsInternal(specification: Specifications, checkApiKeyUniqueness: Boolean, badges: BadgeInfo, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Long] = {
    for {
      replaceApiKey <- if (checkApiKeyUniqueness) {
        PersistenceSchema.specifications.filter(_.apiKey === specification.apiKey).exists.result
      } else {
        DBIO.successful(false)
      }
      newSpecId <- {
        val specToUse = if (replaceApiKey) specification.withApiKey(CryptoUtil.generateApiKey()) else specification
        PersistenceSchema.specifications.returning(PersistenceSchema.specifications.map(_.id)) += specToUse
      }
      _ <- {
        onSuccessCalls += (() => updateSpecificationBadges(newSpecId, badges.forWeb, badges.forReport))
        DBIO.successful(())
      }
    } yield newSpecId
  }

  def createSpecifications(specification: Specifications, badges: BadgeInfo): Long = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = createSpecificationsInternal(specification, checkApiKeyUniqueness = false, badges, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }


}
