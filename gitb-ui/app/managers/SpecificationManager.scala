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

package managers

import exceptions.{AutomationApiException, ErrorCodes}
import models.Enums.TestResultStatus
import models._
import models.automation.{CreateSpecificationGroupRequest, CreateSpecificationRequest, UpdateSpecificationGroupRequest, UpdateSpecificationRequest}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, RepositoryUtils}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SpecificationManager @Inject() (repositoryUtils: RepositoryUtils,
                                      testSuiteManager: TestSuiteManager,
                                      actorManager: ActorManager,
                                      testResultManager: TestResultManager,
                                      automationApiHelper: AutomationApiHelper,
                                      dbConfigProvider: DatabaseConfigProvider)
                                     (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def deleteSpecificationByDomain(domainId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val action = (for {
      ids <- PersistenceSchema.specifications.filter(_.domain === domainId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => deleteSpecificationInternal(id, onSuccessCalls)): _*)
    } yield ()).transactionally
    action
  }

  def deleteSpecification(specId: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = deleteSpecificationInternal(specId, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally).map(_ => ())
  }

  def deleteSpecificationThroughAutomationApi(specificationApiKey: String, communityApiKey: String): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunity(communityApiKey)
      specificationId <- PersistenceSchema.specifications
        .filterOpt(domainId)((q, id) => q.domain === id)
        .filter(_.apiKey === specificationApiKey)
        .map(_.id)
        .result
        .headOption
      _ <- {
        if (specificationId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_SPECIFICATION_NOT_FOUND, "No specification found for the provided API keys")
        } else {
          deleteSpecificationInternal(specificationId.get, onSuccessCalls)
        }
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
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

  def getSpecificationGroups(domainId: Long): Future[List[SpecificationGroups]] = {
    getSpecificationGroupsByDomainIds(Some(List(domainId)))
  }

  def getSpecificationGroupsByDomainIds(domainIds: Option[List[Long]]): Future[List[SpecificationGroups]] = {
    DB.run(
      PersistenceSchema.specificationGroups
        .filterOpt(domainIds)((q, ids) => q.domain inSet ids)
        .sortBy(_.shortname.asc)
        .result
    ).map(_.toList)
  }

  def getSpecificationGroupById(groupId: Long): Future[SpecificationGroups] = {
    DB.run(PersistenceSchema.specificationGroups.filter(_.id === groupId).result.head)
  }

  def createSpecificationGroup(group: SpecificationGroups): Future[Long] = {
    DB.run(createSpecificationGroupInternal(group, checkApiKeyUniqueness = false).transactionally)
  }

  def createSpecificationGroupThroughAutomationApi(input: CreateSpecificationGroupRequest): Future[String] = {
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunityOrDomainApiKey(input.communityApiKey, input.domainApiKey)
      apiKeyToUse <- {
        for {
          generateApiKey <- if (input.apiKey.isEmpty) {
            DBIO.successful(true)
          } else {
            PersistenceSchema.specificationGroups.filter(_.apiKey === input.apiKey.get).exists.result
          }
          apiKeyToUse <- if (generateApiKey) {
            DBIO.successful(CryptoUtil.generateApiKey())
          } else {
            DBIO.successful(input.apiKey.get)
          }
        } yield apiKeyToUse
      }
      _ <- {
        createSpecificationGroupInternal(SpecificationGroups(0L, input.shortName, input.fullName, input.description, input.reportMetadata, input.displayOrder.getOrElse(0), apiKeyToUse, domainId), checkApiKeyUniqueness = false)
      }
    } yield apiKeyToUse
    DB.run(action.transactionally)
  }

  def deleteSpecificationGroupThroughAutomationApi(groupApiKey: String, communityApiKey: String): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunity(communityApiKey)
      groupId <- PersistenceSchema.specificationGroups
        .filterOpt(domainId)((q, id) => q.domain === id)
        .filter(_.apiKey === groupApiKey)
        .map(_.id)
        .result
        .headOption
      _ <- {
        if (groupId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_SPECIFICATION_GROUP_NOT_FOUND, "No specification group found for the provided API keys")
        } else {
          deleteSpecificationGroupInternal(groupId.get, deleteSpecifications = false, onSuccessCalls)
        }
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
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

  def updateSpecificationGroupThroughAutomationApi(updateRequest: UpdateSpecificationGroupRequest): Future[Unit] = {
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunity(updateRequest.communityApiKey)
      group <- PersistenceSchema.specificationGroups
        .filterOpt(domainId)((q, id) => q.domain === id)
        .filter(_.apiKey === updateRequest.groupApiKey)
        .result
        .headOption
      _ <- {
        if (group.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_SPECIFICATION_GROUP_NOT_FOUND, "No specification group found for the provided API keys")
        } else {
          updateSpecificationGroupInternal(
            group.get.id,
            updateRequest.shortName.getOrElse(group.get.shortname),
            updateRequest.fullName.getOrElse(group.get.fullname),
            updateRequest.description.getOrElse(group.get.description),
            updateRequest.reportMetadata.getOrElse(group.get.reportMetadata),
            updateRequest.displayOrder,
            None,
            checkApiKeyUniqueness = false
          )
        }
      }
    } yield ()
    DB.run(action.transactionally)
  }

  def updateSpecificationGroup(groupId: Long, shortname: String, fullname: String, description: Option[String], reportMetadata: Option[String]): Future[Unit] = {
    DB.run(updateSpecificationGroupInternal(groupId, shortname, fullname, description, reportMetadata, None, None, checkApiKeyUniqueness = false).transactionally).map(_ => ())
  }

  def updateSpecificationGroupInternal(groupId: Long, shortname: String, fullname: String, description: Option[String], reportMetadata: Option[String], displayOrder: Option[Short], apiKey: Option[String], checkApiKeyUniqueness: Boolean): DBIO[_] = {
    for {
      _ <- PersistenceSchema.specificationGroups
        .filter(_.id === groupId)
        .map(x => (x.shortname, x.fullname, x.description, x.reportMetadata))
        .update(shortname, fullname, description, reportMetadata)
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

  def removeSpecificationFromGroup(specificationId: Long): Future[Unit] = {
    DB.run(PersistenceSchema.specifications.filter(_.id === specificationId).map(x => (x.group, x.displayOrder)).update(None, 0).transactionally).map(_ => ())
  }

  def addSpecificationToGroup(specificationId: Long, groupId: Long): Future[Unit] = {
    DB.run(PersistenceSchema.specifications.filter(_.id === specificationId).map(x => (x.group, x.displayOrder)).update(Some(groupId), 0).transactionally).map(_ => ())
  }

  def copySpecificationToGroup(specificationId: Long, groupId: Long): Future[Long] = {
    DB.run((
      for {
        // Load existing specification.
        spec <- PersistenceSchema.specifications.filter(_.id === specificationId).result.head
        // Create copy.
        newSpecId <- PersistenceSchema.insertSpecification += Specifications(
          0L, spec.shortname, spec.fullname, spec.description, spec.reportMetadata,
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
        0L, actor.actorId, actor.name, actor.description, actor.reportMetadata,
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

  def deleteSpecificationGroup(groupId: Long, deleteSpecifications: Boolean): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteSpecificationGroupInternal(groupId, deleteSpecifications, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
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
  def checkSpecificationExists(specId: Long): Future[Boolean] = {
    DB.run(
      PersistenceSchema.specifications
        .filter(_.id === specId)
        .exists
        .result
    )
  }

  def getSpecificationById(specId: Long): Future[Specifications] = {
    getSpecificationsById(List(specId)).map(_.head)
  }

  def getSpecificationsById(specIds: List[Long]): Future[List[Specifications]] = {
    DB.run(PersistenceSchema.specifications.filter(_.id inSet specIds).result.map(_.toList))
  }

  def getSpecificationInfoByApiKeys(specificationApiKey: Option[String], communityApiKey: String): Future[Option[(Long, Option[Long])]] = { // Domain ID and specification ID
    DB.run(for {
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

  def updateSpecificationInternal(specId: Long, sname: String, fname: String, descr: Option[String], reportMetadata: Option[String], hidden:Boolean, apiKey: Option[String], checkApiKeyUniqueness: Boolean, groupId: Option[Long], displayOrder: Option[Short], badges: Option[BadgeInfo], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      _ <- {
        for {
          _ <- PersistenceSchema.specifications
            .filter(_.id === specId)
            .map(s => (s.shortname, s.fullname, s.description, s.reportMetadata, s.hidden, s.group))
            .update(sname, fname, descr, reportMetadata, hidden, groupId)
          _ <- testResultManager.updateForUpdatedSpecification(specId, sname)
        } yield ()
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
        if (badges.isDefined) {
          onSuccessCalls += (() => updateSpecificationBadges(specId, badges.get.forWeb, badges.get.forReport))
        }
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

  def updateSpecification(specId: Long, sname: String, fname: String, descr: Option[String], reportMetadata: Option[String], hidden:Boolean, groupId: Option[Long], badges: BadgeInfo): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateSpecificationInternal(specId, sname, fname, descr, reportMetadata, hidden, None, checkApiKeyUniqueness = false, groupId, None, Some(badges), onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  def updateSpecificationThroughAutomationApi(updateRequest: UpdateSpecificationRequest): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunity(updateRequest.communityApiKey)
      groupId <- {
        if (updateRequest.groupApiKey.isEmpty) {
          // No change.
          DBIO.successful(None)
        } else if (updateRequest.groupApiKey.get.isEmpty) {
          // Remove specification from current group (if set).
          DBIO.successful(Some(None))
        } else {
          // Update group to match the one of the provided API key.
          for {
            groupId <- getGroupIdToUseForApiKey(domainId, updateRequest.groupApiKey.get)
          } yield Some(groupId)
        }
      }
      specification <- PersistenceSchema.specifications
        .filterOpt(domainId)((q, id) => q.domain === id)
        .filter(_.apiKey === updateRequest.specificationApiKey)
        .result
        .headOption
      _ <- {
        if (specification.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_SPECIFICATION_NOT_FOUND, "No specification found for the provided API keys")
        } else {
          updateSpecificationInternal(
            specification.get.id,
            updateRequest.shortName.getOrElse(specification.get.shortname),
            updateRequest.fullName.getOrElse(specification.get.fullname),
            updateRequest.description.getOrElse(specification.get.description),
            updateRequest.reportMetadata.getOrElse(specification.get.reportMetadata),
            updateRequest.hidden.getOrElse(specification.get.hidden),
            None,
            checkApiKeyUniqueness = false,
            groupId.getOrElse(specification.get.group),
            updateRequest.displayOrder,
            None,
            onSuccessCalls
          )
        }
      }
    } yield ()
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }


  def getSpecificationIdOfActor(actorId: Long): Future[Long] = {
    DB.run(
      PersistenceSchema.specificationHasActors
        .filter(_.actorId === actorId)
        .map(_.specId)
        .result
        .head
    )
  }

  def getSpecificationOfActor(actorId: Long): Future[Specifications] = {
    DB.run(getSpecificationOfActorInternal(actorId))
  }

  def getSpecificationOfActorInternal(actorId: Long): DBIO[Specifications] = {
    PersistenceSchema.specifications
      .join(PersistenceSchema.specificationHasActors).on(_.id === _.specId)
      .filter(_._2.actorId === actorId)
      .map(_._1)
      .result
      .head
  }

  def saveSpecificationOrder(groupIds: List[Long], groupOrders: List[Long], specIds: List[Long], specOrders: List[Long]): Future[Unit] = {
    val dbActions = new ListBuffer[DBIO[_]]
    for (i <- groupIds.indices) {
      dbActions += PersistenceSchema.specificationGroups.filter(_.id === groupIds(i)).map(_.displayOrder).update(groupOrders(i).toShort)
    }
    for (i <- specIds.indices) {
      dbActions += PersistenceSchema.specifications.filter(_.id === specIds(i)).map(_.displayOrder).update(specOrders(i).toShort)
    }
    DB.run(toDBIO(dbActions)).map(_ => ())
  }

  def resetSpecificationOrder(domainId: Long): Future[Unit] = {
    DB.run(
      (for {
        _ <- PersistenceSchema.specifications.filter(_.domain === domainId).map(_.displayOrder).update(0)
        _ <- PersistenceSchema.specificationGroups.filter(_.domain === domainId).map(_.displayOrder).update(0)
      } yield ()).transactionally
    )
  }

  private def mergeSpecsWithGroups(data: Seq[(Specifications, Option[SpecificationGroups])]): Seq[Specifications] = {
    // First map specification options to groups
    val groupSpecs = mutable.HashMap[Long, ListBuffer[Specifications]]()
    data.foreach { specData =>
      if (specData._2.nonEmpty) {
        var specList = groupSpecs.get(specData._2.get.id)
        if (specList.isEmpty) {
          specList = Some(ListBuffer[Specifications]())
          groupSpecs += (specData._2.get.id -> specList.get)
        }
        specList.get += specData._1
      }
    }
    // Now sort top-level specifications and groups together.
    var specsOrGroups = ListBuffer[Specifications]()
    val addedGroups = mutable.HashSet[Long]()
    data.foreach { specData =>
      if (specData._2.nonEmpty) {
        if (!addedGroups.contains(specData._2.get.id) && groupSpecs.contains(specData._2.get.id)) {
          // Create a placeholder specification with the group's data for the sort
          specsOrGroups += Specifications(0L, specData._2.get.shortname, specData._2.get.fullname, None, None, hidden = false, specData._2.get.apiKey, specData._2.get.domain, specData._2.get.displayOrder, Some(specData._2.get.id))
          // Remember we added this group as we want to add it only once
          addedGroups += specData._2.get.id
        }
      } else {
        specsOrGroups += specData._1
      }
    }
    specsOrGroups = specsOrGroups.sortBy(x => (x.displayOrder, x.shortname))
    // Replace the group placeholders with the options that we sort in the context of each group
    val specsToReturn = ListBuffer[Specifications]()
    specsOrGroups.foreach { specData =>
      if (specData.group.isEmpty) {
        specsToReturn += specData
      } else {
        val sortedOptions = groupSpecs(specData.group.get).sortBy(x => (x.displayOrder, x.shortname))
        sortedOptions.foreach { option =>
          specsToReturn += option.withGroupNames(specData.shortname, specData.fullname)
        }
      }
    }
    specsToReturn.toList
  }

  def getSpecificationsInternal(ids: Option[Iterable[Long]] = None, domainIds: Option[List[Long]] = None, groupIds: Option[List[Long]] = None, withGroups: Boolean = false): DBIO[Seq[Specifications]] = {
    if (withGroups) {
      for {
        specData <- PersistenceSchema.specifications
          .joinLeft(PersistenceSchema.specificationGroups).on(_.group === _.id)
          .filterOpt(ids)((q, ids) => q._1.id inSet ids)
          .filterOpt(domainIds)((q, domainIds) => q._1.domain inSet domainIds)
          .filterOpt(groupIds)((q, groupIds) => q._1.group inSet groupIds)
          .map(x => (x._1, x._2))
          .result
        mergedData <- DBIO.successful(mergeSpecsWithGroups(specData))
      } yield mergedData
    } else {
      PersistenceSchema.specifications
        .filterOpt(ids)((q, ids) => q.id inSet ids)
        .filterOpt(domainIds)((q, domainIds) => q.domain inSet domainIds)
        .filterOpt(groupIds)((q, groupIds) => q.group inSet groupIds)
        .sortBy(x => (x.displayOrder.asc, x.shortname.asc))
        .result
    }
  }

  def getSpecifications(ids: Option[Iterable[Long]] = None, domainIds: Option[List[Long]] = None, groupIds: Option[List[Long]] = None, withGroups: Boolean = false): Future[Seq[Specifications]] = {
    DB.run(getSpecificationsInternal(ids, domainIds, groupIds, withGroups))
  }

  def getSpecifications(domain: Long, withGroups: Boolean): Future[Seq[Specifications]] = {
    val action = if (withGroups) {
      for {
        specData <- PersistenceSchema.specifications
          .joinLeft(PersistenceSchema.specificationGroups).on(_.group === _.id)
          .filter(_._1.domain === domain)
          .map(x => (x._1, x._2))
          .result
        specsWithGroups <- DBIO.successful(mergeSpecsWithGroups(specData))
      } yield specsWithGroups
    } else {
      PersistenceSchema.specifications
        .filter(_.domain === domain)
        .sortBy(x => (x.displayOrder.asc, x.shortname.asc))
        .result
    }
    DB.run(action)
  }

  def getSpecificationsLinkedToTestSuite(testSuiteId: Long): Future[Seq[Specifications]] = {
    DB.run(
      for {
        specificationIds <- PersistenceSchema.specificationHasTestSuites.filter(_.testSuiteId === testSuiteId).map(_.specId).result
        specifications <- getSpecificationsInternal(Some(specificationIds), None, withGroups = true)
      } yield specifications
    )
  }

  def getSpecificationsForTestSuiteMove(testSuiteId: Long): Future[Seq[Specifications]] = {
    DB.run(
      for {
        testSuiteInfo <- PersistenceSchema.testSuites
          .filter(_.id === testSuiteId)
          .map(x => (x.identifier, x.domain))
          .result
          .head
        // Get the specifications linked to the domain.
        domainSpecifications <- PersistenceSchema.specifications
          .filter(_.domain === testSuiteInfo._2)
          .map(_.id)
          .result
        // Get the specification IDs that include a test suite with the provided one's identifier.
        specificationsLinkedToTestSuiteIdentifier <- PersistenceSchema.specificationHasTestSuites
          .join(PersistenceSchema.testSuites).on(_.testSuiteId === _.id)
          .filter(_._2.identifier === testSuiteInfo._1)
          .map(_._1.specId)
          .result
        // For the specifications not including the test suite's identifier, load their data.
        specifications <- {
          val specificationIds = domainSpecifications.toSet.removedAll(specificationsLinkedToTestSuiteIdentifier)
          getSpecificationsInternal(Some(specificationIds), None, withGroups = true)
        }
      } yield specifications
    )
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

  def createSpecifications(specification: Specifications, badges: BadgeInfo): Future[Long] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = createSpecificationsInternal(specification, checkApiKeyUniqueness = false, badges, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  private def getGroupIdToUseForApiKey(domainId: Option[Long], groupApiKey: Option[String]): DBIO[Option[Long]] = {
    if (groupApiKey.isDefined) {
      for {
        groupIdToUse <- PersistenceSchema.specificationGroups
          .filterOpt(domainId)((q, id) => q.domain === id)
          .filter(_.apiKey === groupApiKey.get)
          .map(_.id)
          .result
          .headOption
        _ <- {
          if (groupIdToUse.isEmpty) {
            throw AutomationApiException(ErrorCodes.API_SPECIFICATION_GROUP_NOT_FOUND, "No specification group found for the provided API keys")
          } else {
            DBIO.successful(())
          }
        }
      } yield groupIdToUse
    } else {
      DBIO.successful(None)
    }
  }

  def createSpecificationThroughAutomationApi(input: CreateSpecificationRequest): Future[String] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunityOrDomainApiKey(input.communityApiKey, input.domainApiKey)
      apiKeyToUse <- {
        for {
          generateApiKey <- if (input.apiKey.isEmpty) {
            DBIO.successful(true)
          } else {
            PersistenceSchema.specifications.filter(_.apiKey === input.apiKey.get).exists.result
          }
          apiKeyToUse <- if (generateApiKey) {
            DBIO.successful(CryptoUtil.generateApiKey())
          } else {
            DBIO.successful(input.apiKey.get)
          }
        } yield apiKeyToUse
      }
      groupIdToUse <- getGroupIdToUseForApiKey(Some(domainId), input.groupApiKey)
      _ <- {
        createSpecificationsInternal(Specifications(0L, input.shortName, input.fullName, input.description, input.reportMetadata,
          input.hidden.getOrElse(false), apiKeyToUse, domainId, input.displayOrder.getOrElse(0), groupIdToUse
        ), checkApiKeyUniqueness = false, BadgeInfo.noBadges(), onSuccessCalls)
      }
    } yield apiKeyToUse
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

}
