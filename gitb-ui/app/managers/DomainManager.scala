package managers

import models.Domain
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.{CryptoUtil, RepositoryUtils}

import javax.inject.{Inject, Singleton}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DomainManager @Inject() (domainParameterManager: DomainParameterManager, repositoryUtils: RepositoryUtils, conformanceManager: ConformanceManager, testSuiteManager: TestSuiteManager, specificationManager: SpecificationManager, testResultManager: TestResultManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

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

  def createDomainInternal(domain: Domain, checkApiKeyUniqueness: Boolean): DBIO[Long] = {
    for {
      replaceApiKey <- {
        if (checkApiKeyUniqueness) {
          PersistenceSchema.domains.filter(_.apiKey === domain.apiKey).exists.result
        } else {
          DBIO.successful(false)
        }
      }
      newDomainId <- {
        if (replaceApiKey) {
          PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain.withApiKey(CryptoUtil.generateApiKey())
        } else {
          PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain
        }
      }
    } yield newDomainId
  }

  def createDomain(domain: Domain): Long = {
    exec(createDomainInternal(domain, checkApiKeyUniqueness = false))
  }

  def updateDomain(domainId: Long, shortName: String, fullName: String, description: Option[String]): Unit = {
    exec(updateDomainInternal(domainId, shortName, fullName, description, None).transactionally)
  }

  def updateDomainInternal(domainId: Long, shortName: String, fullName: String, description: Option[String], apiKey: Option[String]): DBIO[_] = {
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
            .map(x => (x.shortname, x.fullname, x.description, x.apiKey))
            .update((shortName, fullName, description, apiKeyToUse))
        } else {
          PersistenceSchema.domains.filter(_.id === domainId)
            .map(x => (x.shortname, x.fullname, x.description))
            .update((shortName, fullName, description))
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

  private def deleteTransactionByDomain(domainId: Long) = {
    PersistenceSchema.transactions.filter(_.domain === domainId).delete
  }

}
