package managers

import exceptions.{AutomationApiException, ErrorCodes}
import models.Domain
import models.automation.UpdateDomainRequest
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, RepositoryUtils}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DomainManager @Inject() (domainParameterManager: DomainParameterManager,
                               repositoryUtils: RepositoryUtils,
                               conformanceManager: ConformanceManager,
                               testSuiteManager: TestSuiteManager,
                               specificationManager: SpecificationManager,
                               testResultManager: TestResultManager,
                               automationApiHelper: AutomationApiHelper,
                               dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getDomainById(id: Long): Domain = {
    exec(PersistenceSchema.domains.filter(_.id === id).result.head)
  }

  def getByApiKey(apiKey: String): Option[Domain] = {
    exec(PersistenceSchema.domains.filter(_.apiKey === apiKey).result.headOption)
  }

  /**
   * Checks if domain exists
   */
  def checkDomainExists(domainId: Long): Boolean = {
    exec(PersistenceSchema.domains.filter(_.id === domainId).result.headOption).isDefined
  }

  def getDomains(ids: Option[List[Long]] = None): List[Domain] = {
    exec(
      PersistenceSchema.domains
        .filterOpt(ids)((q, ids) => q.id inSet ids)
        .sortBy(_.shortname.asc)
        .result
        .map(_.toList)
    )
  }

  def getCommunityDomain(communityId: Long): Option[Domain] = {
    exec(
      PersistenceSchema.communities
      .join(PersistenceSchema.domains).on(_.domain === _.id)
      .filter(_._1.id === communityId)
      .map(_._2)
      .result
      .headOption
    )
  }

  def getDomainOfActor(actorId: Long): Domain = {
    exec(
      PersistenceSchema.actors
        .join(PersistenceSchema.domains).on(_.domain === _.id)
        .filter(_._1.id === actorId)
        .map(x => x._2)
        .result
        .head
    )
  }

  def getDomainOfSpecification(specificationId: Long): Domain = {
    exec(
      PersistenceSchema.domains
        .join(PersistenceSchema.specifications).on(_.id === _.domain)
        .filter(_._2.id === specificationId)
        .map(x => x._1)
        .result
        .head
    )
  }

  def createDomainForImport(domain: Domain): DBIO[Long] = {
    for {
      result <- createDomainInternal(domain, checkApiKeyUniqueness = true)
    } yield result._1
  }

  private def createDomainInternal(domain: Domain, checkApiKeyUniqueness: Boolean): DBIO[(Long, String)] = {
    for {
      replaceApiKey <- {
        if (checkApiKeyUniqueness) {
          PersistenceSchema.domains.filter(_.apiKey === domain.apiKey).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      apiKeyToUse <- {
        if (replaceApiKey) {
          DBIO.successful(CryptoUtil.generateApiKey())
        } else {
          DBIO.successful(domain.apiKey)
        }
      }
      newDomainId <- {
        if (replaceApiKey) {
          PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain.withApiKey(apiKeyToUse)
        } else {
          PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain
        }
      }
    } yield (newDomainId, apiKeyToUse)
  }

  def createDomain(domain: Domain): Long = {
    exec(createDomainInternal(domain, checkApiKeyUniqueness = false))._1
  }

  def createDomain(domain: Domain, checkApiKeyUniqueness: Boolean): String = {
    exec(createDomainInternal(domain, checkApiKeyUniqueness))._2
  }

  def updateDomain(domainId: Long, shortName: String, fullName: String, description: Option[String], reportMetadata: Option[String]): Unit = {
    exec(updateDomainInternal(domainId, shortName, fullName, description, reportMetadata, None).transactionally)
  }

  def updateDomainThroughAutomationApi(updateRequest: UpdateDomainRequest): Unit = {
    val action = for {
      domain <- {
        if (updateRequest.domainApiKey.isDefined) {
          PersistenceSchema.domains
            .filter(_.apiKey === updateRequest.domainApiKey.get)
            .result
            .headOption
        } else if (updateRequest.communityApiKey.isDefined) {
          PersistenceSchema.communities
            .join(PersistenceSchema.domains).on(_.domain === _.id)
            .filter(_._1.apiKey === updateRequest.communityApiKey.get)
            .filter(_._1.domain.isDefined)
            .map(_._2)
            .result
            .headOption
        } else {
          DBIO.successful(None)
        }
      }
      _ <- {
        if (domain.isEmpty) {
          throw AutomationApiException(ErrorCodes.API_DOMAIN_NOT_FOUND, "No domain found for the provided API key")
        } else {
          PersistenceSchema.domains.filter(_.id === domain.get.id).map(x => (x.shortname, x.fullname, x.description, x.reportMetadata)).update((
            updateRequest.shortName.getOrElse(domain.get.shortname),
            updateRequest.fullName.getOrElse(domain.get.fullname),
            updateRequest.description.getOrElse(domain.get.description),
            updateRequest.reportMetadata.getOrElse(domain.get.reportMetadata)
          ))
        }
      }
    } yield ()
    exec(action.transactionally)
  }

  def updateDomainInternal(domainId: Long, shortName: String, fullName: String, description: Option[String], reportMetadata: Option[String], apiKey: Option[String]): DBIO[_] = {
    for {
      replaceApiKey <- {
        if (apiKey.isDefined) {
          PersistenceSchema.domains.filter(_.apiKey === apiKey.get).filter(_.id =!= domainId).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      _ <- {
        if (apiKey.isDefined) {
          val apiKeyToUse = if (replaceApiKey) CryptoUtil.generateApiKey() else apiKey.get
          PersistenceSchema.domains.filter(_.id === domainId)
            .map(x => (x.shortname, x.fullname, x.description, x.reportMetadata, x.apiKey))
            .update((shortName, fullName, description, reportMetadata, apiKeyToUse))
        } else {
          PersistenceSchema.domains.filter(_.id === domainId)
            .map(x => (x.shortname, x.fullname, x.description, x.reportMetadata))
            .update((shortName, fullName, description, reportMetadata))
        }
      }
      _ <- {
        testResultManager.updateForUpdatedDomain(domainId, shortName)
      }
    } yield ()
  }

  private def removeDomainFromCommunities(domainId: Long, onSuccess: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      communityIds <- PersistenceSchema.communities.filter(_.domain === domainId).map(x => x.id).result
      _ <- {
        val actions = ListBuffer[DBIO[_]]()
        communityIds.foreach { communityId =>
          // Delete conformance statements
          actions += conformanceManager.deleteConformanceStatementsForDomainAndCommunity(communityId, domainId, onSuccess)
          // Set domain of community to null
          actions += (for {x <- PersistenceSchema.communities if x.domain === domainId} yield x.domain).update(None)
        }
        toDBIO(actions)
      }
    } yield ()
  }

  def deleteDomainInternal(domain: Long, onSuccessCalls: ListBuffer[() => _]): DBIO[_] = {
    for {
      _ <- removeDomainFromCommunities(domain, onSuccessCalls)
      _ <- specificationManager.deleteSpecificationByDomain(domain, onSuccessCalls)
      _ <- PersistenceSchema.specificationGroups.filter(_.domain === domain).delete
      sharedTestSuiteIds <- PersistenceSchema.testSuites.filter(_.domain === domain).map(_.id).result
      _ <- DBIO.seq(sharedTestSuiteIds.map(testSuiteManager.undeployTestSuite(_, onSuccessCalls)): _*)
      _ <- deleteTransactionByDomain(domain)
      _ <- testResultManager.updateForDeletedDomain(domain)
      _ <- domainParameterManager.deleteDomainParameters(domain, onSuccessCalls)
      _ <- PersistenceSchema.conformanceOverviewCertificateMessages.filter(row => row.domain.isDefined && row.domain === domain).delete
      _ <- PersistenceSchema.conformanceSnapshotResults.filter(_.domainId === domain).map(_.domainId).update(domain * -1)
      _ <- PersistenceSchema.conformanceSnapshotDomains.filter(_.id === domain).map(_.id).update(domain * -1)
      _ <- PersistenceSchema.conformanceSnapshotDomainParameters.filter(_.domainId === domain).map(_.domainId).update(domain * -1)
      _ <- PersistenceSchema.conformanceSnapshotOverviewCertificateMessages.filter(row => row.domainId.isDefined && row.domainId === domain).map(_.domainId).update(Some(domain * -1))
      _ <- PersistenceSchema.domains.filter(_.id === domain).delete
      _ <- {
        onSuccessCalls += (() => {
          repositoryUtils.deleteDomainTestSuiteFolder(domain)
        })
        DBIO.successful(())
      }
    } yield ()
  }

  def deleteDomain(domain: Long): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = deleteDomainInternal(domain, onSuccessCalls)
    exec(
      dbActionFinalisation(Some(onSuccessCalls), None, action)
        .transactionally
    )
  }

  def deleteDomainThroughAutomationApi(apiKey: String): Unit = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val action = for {
      domainId <- automationApiHelper.getDomainIdByDomainApiKey(apiKey)
      _ <- deleteDomainInternal(domainId, onSuccessCalls)
    } yield ()
    exec(dbActionFinalisation(Some(onSuccessCalls), None, action).transactionally)
  }

  private def deleteTransactionByDomain(domainId: Long) = {
    PersistenceSchema.transactions.filter(_.domain === domainId).delete
  }

}
