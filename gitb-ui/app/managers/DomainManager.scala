package managers

import models.Domain
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.RepositoryUtils

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

  def createDomainInternal(domain: Domain): DBIO[Long] = {
    PersistenceSchema.domains.returning(PersistenceSchema.domains.map(_.id)) += domain
  }

  def createDomain(domain: Domain): Long = {
    exec(createDomainInternal(domain))
  }

  def updateDomain(domainId: Long, shortName: String, fullName: String, description: Option[String]) = {
    exec(updateDomainInternal(domainId, shortName, fullName, description).transactionally)
  }

  def updateDomainInternal(domainId: Long, shortName: String, fullName: String, description: Option[String]) = {
    val q = for {d <- PersistenceSchema.domains if d.id === domainId} yield (d.shortname, d.fullname, d.description)
    q.update(shortName, fullName, description) andThen
      testResultManager.updateForUpdatedDomain(domainId, shortName)
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
      _ <- PersistenceSchema.conformanceSnapshotResults.filter(_.domainId === domain).map(_.domainId).update(domain * -1)
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
