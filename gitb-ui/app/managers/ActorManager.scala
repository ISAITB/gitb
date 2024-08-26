package managers

import exceptions.{AutomationApiException, ErrorCodes}
import models.Enums.TestResultStatus
import models.automation.{CreateActorRequest, UpdateActorRequest}

import javax.inject.{Inject, Singleton}
import models.{Actor, Actors, BadgeInfo, Badges}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIOAction
import utils.{CryptoUtil, RepositoryUtils}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ActorManager @Inject() (repositoryUtils: RepositoryUtils,
                              testResultManager: TestResultManager,
                              endPointManager: EndPointManager,
                              optionManager: OptionManager,
                              automationApiHelper: AutomationApiHelper,
                              dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  /**
   * Checks if actor exists
   */
  def checkActorExistsInSpecification(actorId: String, specificationId: Long, otherThanId: Option[Long]): Boolean = {
    var query = PersistenceSchema.actors
      .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
    query = query
      .filter(_._1.actorId === actorId)
      .filter(_._2.specId === specificationId)
    if (otherThanId.isDefined) {
      query = query.filter(_._1.id =!= otherThanId.get)
    }
    val actor = query.result.headOption
    exec(actor).isDefined
  }

  def deleteActorWrapper(actorId: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = deleteActor(actorId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def deleteActor(actorId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    delete(actorId, onSuccessCalls)
  }

  def deleteActorThroughAutomationApi(actorApiKey: String, communityApiKey: String): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunity(communityApiKey)
      actorId <- PersistenceSchema.actors
        .filterOpt(domainId)((q, id) => q.domain === id)
        .filter(_.apiKey === actorApiKey)
        .map(_.id)
        .result
        .headOption
      _ <- {
        if (actorId.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_ACTOR_NOT_FOUND, "No actor found for the provided API keys")
        } else {
          deleteActor(actorId.get, onSuccessCalls)
        }
      }
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  private def delete(actorId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      specificationId <- PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).map(_.specId).result.head
      _ <- {
        testResultManager.updateForDeletedActor(actorId) andThen
          PersistenceSchema.testCaseHasActors.filter(_.actor === actorId).delete andThen
          PersistenceSchema.testSuiteHasActors.filter(_.actor === actorId).delete andThen
          PersistenceSchema.systemImplementsActors.filter(_.actorId === actorId).delete andThen
          PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).delete andThen
          PersistenceSchema.endpointSupportsTransactions.filter(_.actorId === actorId).delete andThen
          endPointManager.deleteEndPointByActor(actorId, onSuccessCalls) andThen
          optionManager.deleteOptionByActor(actorId) andThen
          PersistenceSchema.conformanceResults.filter(_.actor === actorId).delete andThen
          PersistenceSchema.conformanceSnapshotResults.filter(_.actorId === actorId).map(_.actorId).update(actorId * -1) andThen
          PersistenceSchema.conformanceSnapshotActors.filter(_.id === actorId).map(_.id).update(actorId * -1) andThen
          PersistenceSchema.actors.filter(_.id === actorId).delete andThen {
            onSuccessCalls += (() => repositoryUtils.deleteActorBadges(specificationId, actorId))
            DBIO.successful(())
        }
      }
    } yield ()
  }

  def updateActorWrapper(id: Long, actorId: String, name: String, description: Option[String], default: Option[Boolean], hidden: Boolean, displayOrder: Option[Short], specificationId: Long, badges: BadgeInfo): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = updateActor(id, actorId, name, description, default, hidden, displayOrder, specificationId, None, checkApiKeyUniqueness = false, Some(badges), onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def updateActorThroughAutomationApi(updateRequest: UpdateActorRequest): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunity(updateRequest.communityApiKey)
      // Load existing actor information (actor data and specification ID).
      actorInfo <- {
        for {
          actorInfo <- PersistenceSchema.actors
            .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
            .filterOpt(domainId)((q, id) => q._1.domain === id)
            .filter(_._1.apiKey === updateRequest.actorApiKey)
            .map(x => (x._1, x._2.specId))
            .result
            .headOption
          _ <- {
            if (actorInfo.isEmpty) {
              throw AutomationApiException(ErrorCodes.API_ACTOR_NOT_FOUND, "No actor found for the provided API keys")
            } else {
              DBIO.successful(())
            }
          }
        } yield actorInfo.get
      }
      // If the identifier has changed ensure it remains unique.
      _ <- {
        if (updateRequest.identifier.isDefined && updateRequest.identifier.get != actorInfo._1.actorId) {
          ensureActorIdentifierIsUnique(actorInfo._2, updateRequest.identifier.get, Some(actorInfo._1.id))
        } else {
          DBIO.successful(())
        }
      }
      _ <- updateActor(
        actorInfo._1.id,
        updateRequest.identifier.getOrElse(actorInfo._1.actorId),
        updateRequest.name.getOrElse(actorInfo._1.name),
        updateRequest.description.getOrElse(actorInfo._1.description),
        updateRequest.default.getOrElse(actorInfo._1.default),
        updateRequest.hidden.getOrElse(actorInfo._1.hidden),
        updateRequest.displayOrder.getOrElse(actorInfo._1.displayOrder),
        actorInfo._2,
        None,
        checkApiKeyUniqueness = false,
        None,
        onSuccessCalls
      )
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def updateActor(id: Long, actorId: String, name: String, description: Option[String], default: Option[Boolean], hidden: Boolean, displayOrder: Option[Short], specificationId: Long, apiKey: Option[String], checkApiKeyUniqueness: Boolean, badges: Option[BadgeInfo], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    var defaultToSet: Option[Boolean] = null
    if (default.isEmpty) {
      defaultToSet = Some(false)
    } else {
      defaultToSet = default
    }
    (for  {
      _ <- {
        val q1 = for {a <- PersistenceSchema.actors if a.id === id} yield (a.name, a.desc, a.actorId, a.default, a.hidden, a.displayOrder)
        q1.update((name, description, actorId, defaultToSet, hidden, displayOrder))
      }
      replaceApiKey <- {
        if (apiKey.isDefined && checkApiKeyUniqueness) {
          PersistenceSchema.actors.filter(_.apiKey === apiKey.get).filter(_.id =!= id).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      _ <- {
        if (apiKey.isDefined) {
          val apiKeyToUse = if (replaceApiKey) CryptoUtil.generateApiKey() else apiKey.get
          PersistenceSchema.actors.filter(_.id === id).map(_.apiKey).update(apiKeyToUse)
        } else {
          DBIO.successful(())
        }
      }
      _ <- {
        if (default.isDefined && default.get) {
          // Ensure no other default actors are defined.
          setOtherActorsAsNonDefault(id, specificationId)
        } else {
          DBIOAction.successful(())
        }
      }
      _ <- {
        if (badges.isDefined) {
          onSuccessCalls += (() => updateActorBadges(specificationId, id, badges.get.forWeb, badges.get.forReport))
        }
        DBIO.successful(())
      }
    } yield()) andThen
    testResultManager.updateForUpdatedActor(id, name)
  }

  def getById(id: Long): Option[Actors] = {
    exec(PersistenceSchema.actors.filter(_.id === id).result.headOption)
  }

  private def setOtherActorsAsNonDefault(defaultActorId: Long, specificationId: Long) = {
    val actions = (for {
      actorIds <- PersistenceSchema.specificationHasActors
        .filter(_.specId === specificationId)
        .map(e => e.actorId)
        .result
      _ <- DBIO.seq(actorIds.map(actorId =>
        if (actorId != defaultActorId) {
          val q = for (a <- PersistenceSchema.actors if a.id === actorId) yield a.default
          q.update(Some(false))
        } else {
          DBIOAction.successful(())
        }
      ): _*)
    } yield()).transactionally
    actions
  }

  def createActorWrapper(actor: Actors, specificationId: Long, badges: BadgeInfo): Long = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = createActor(actor, specificationId, checkApiKeyUniqueness = false, Some(badges), onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  private def ensureActorIdentifierIsUnique(specificationId: Long, actorIdentifier: String, actorIdToIgnore: Option[Long]): DBIO[_] = {
    for {
      actorIdentifierExists <- PersistenceSchema.actors
        .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
        .filter(_._1.actorId === actorIdentifier)
        .filter(_._2.specId === specificationId)
        .filterOpt(actorIdToIgnore)((q, id) => q._1.id =!= id)
        .exists
        .result
      _ <- {
        if (actorIdentifierExists) {
          throw AutomationApiException(ErrorCodes.API_ACTOR_IDENTIFIER_EXISTS, "The specification already defines an actor with the provided identifier")
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  def createActorThroughAutomationApi(input: CreateActorRequest): String = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByCommunity(input.communityApiKey)
      specificationIds <- {
        for {
          specificationIds <- PersistenceSchema.specifications
            .filterOpt(domainId)((q, id) => q.domain === id)
            .filter(_.apiKey === input.specificationApiKey)
            .map(x => (x.id, x.domain))
            .result
            .headOption
          _ <- {
            if (specificationIds.isEmpty) {
              throw AutomationApiException(ErrorCodes.API_SPECIFICATION_NOT_FOUND, "No specification found for the provided API keys")
            } else {
              DBIO.successful(())
            }
          }
        } yield specificationIds.get
      }
      _ <- ensureActorIdentifierIsUnique(specificationIds._1, input.identifier, None)
      apiKeyToUse <- {
        for {
          generateApiKey <- if (input.apiKey.isEmpty) {
            DBIO.successful(true)
          } else {
            PersistenceSchema.actors.filter(_.apiKey === input.apiKey.get).exists.result
          }
          apiKeyToUse <- if (generateApiKey) {
            DBIO.successful(CryptoUtil.generateApiKey())
          } else {
            DBIO.successful(input.apiKey.get)
          }
        } yield apiKeyToUse
      }
      _ <- {
        createActor(
          Actors(0L, input.identifier, input.name, input.description, Some(input.default.getOrElse(false)), input.hidden.getOrElse(false), input.displayOrder, apiKeyToUse, specificationIds._2),
          specificationIds._1,
          checkApiKeyUniqueness = false,
          None,
          onSuccessCalls
        )
      }
    } yield apiKeyToUse
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  def createActor(actor: Actors, specificationId: Long, checkApiKeyUniqueness: Boolean, badges: Option[BadgeInfo], onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[Long] = {
    for {
      replaceApiKey <- if (checkApiKeyUniqueness) {
        PersistenceSchema.actors.filter(_.apiKey === actor.apiKey).exists.result
      } else {
        DBIO.successful(false)
      }
      savedActorId <- {
        val actorToUse = if (replaceApiKey) actor.withApiKey(CryptoUtil.generateApiKey()) else actor
        PersistenceSchema.actors.returning(PersistenceSchema.actors.map(_.id)) += actorToUse
      }
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        actions += (PersistenceSchema.specificationHasActors += (specificationId, savedActorId))
        if (actor.default.isDefined && actor.default.get) {
          // Ensure no other default actors are defined.
          actions += setOtherActorsAsNonDefault(savedActorId, specificationId)
        }
        DBIO.seq(actions.toList.map(a => a): _*)
      }
      _ <- {
        if (badges.isDefined) {
          onSuccessCalls += (() => updateActorBadges(specificationId, savedActorId, badges.get.forWeb, badges.get.forReport))
        }
        DBIO.successful(())
      }
    } yield savedActorId
  }

  private def updateActorBadges(specId: Long, actorId: Long, badges: Badges, badgesForReport: Badges): Unit = {
    // We can either have no badges or at least the success and other ones.
    if ((!badges.hasSuccess && !badges.hasOther && !badges.hasFailure) || (badges.hasSuccess && badges.hasOther)) {
      updateActorBadgesForCase(specId, actorId, badges, forReport = false)
      updateActorBadgesForCase(specId, actorId, badgesForReport, forReport = true)
    }
  }

  private def updateActorBadgesForCase(specId: Long, actorId: Long, badges: Badges, forReport: Boolean): Unit = {
    // Delete previous or removed files.
    if (!badges.hasSuccess || badges.success.isDefined) {
      repositoryUtils.deleteActorBadge(specId, actorId, TestResultStatus.SUCCESS.toString, forReport)
    }
    if (!badges.hasOther || badges.other.isDefined) {
      repositoryUtils.deleteActorBadge(specId, actorId, TestResultStatus.UNDEFINED.toString, forReport)
    }
    if (!badges.hasFailure || badges.failure.isDefined) {
      repositoryUtils.deleteActorBadge(specId, actorId, TestResultStatus.FAILURE.toString, forReport)
    }
    // Add new files.
    if (badges.success.isDefined) repositoryUtils.setActorBadge(specId, actorId, badges.success.get, TestResultStatus.SUCCESS.toString, forReport)
    if (badges.other.isDefined) repositoryUtils.setActorBadge(specId, actorId, badges.other.get, TestResultStatus.UNDEFINED.toString, forReport)
    if (badges.failure.isDefined) repositoryUtils.setActorBadge(specId, actorId, badges.failure.get, TestResultStatus.FAILURE.toString, forReport)
  }

  def searchActors(domainIds: Option[List[Long]], specificationIds: Option[List[Long]], specificationGroupIds: Option[List[Long]]): List[Actor] = {
    exec(
      PersistenceSchema.actors
        .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
        .join(PersistenceSchema.specifications).on(_._2.specId === _.id)
        .filterOpt(domainIds)((q, ids) => q._1._1.domain inSet ids)
        .filterOpt(specificationIds)((q, ids) => q._1._2.specId inSet ids)
        .filterOpt(specificationGroupIds)((q, ids) => q._2.group inSet ids)
        .sortBy(_._1._1.actorId.asc)
        .map(x => (x._1._1, x._1._2.specId))
        .result
    ).map(x => new Actor(x._1, null, null, x._2)).toList
  }

  def getActorIdsOfSpecifications(specIds: List[Long]): Map[Long, Set[String]] = {
    val results = exec(
      PersistenceSchema.actors
        .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
        .filter(_._2.specId inSet specIds)
        .map(x => (x._1.actorId, x._2.specId))
        .result
        .map(_.toList)
    )
    val specMap = mutable.Map[Long, mutable.Set[String]]()
    results.foreach { result =>
      var actorIdSet = specMap.get(result._2)
      if (actorIdSet.isEmpty) {
        actorIdSet = Some(mutable.Set[String]())
        specMap += (result._2 -> actorIdSet.get)
      }
      actorIdSet.get += result._1
    }
    // Add empty sets for spec IDs with no results.
    specIds.foreach { specId =>
      if (!specMap.contains(specId)) {
        specMap += (specId -> mutable.Set[String]())
      }
    }
    specMap.iterator.toMap.map(x => (x._1, x._2.toSet))
  }

  def getActorsWithSpecificationId(actorIds: Option[List[Long]], specIds: Option[List[Long]]): List[Actor] = {
    exec(
      PersistenceSchema.actors
        .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
        .filterOpt(actorIds)((q, ids) => q._1.id inSet ids)
        .filterOpt(specIds)((q, ids) => q._2.specId inSet ids)
        .sortBy(_._1.actorId.asc)
        .map(x => (x._1, x._2.specId))
        .result
    ).map(x => new Actor(x._1, null, null, x._2)).toList
  }

}
