package managers

import javax.inject.{Inject, Singleton}
import models._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class TestResultManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {
  def logger = LoggerFactory.getLogger("TestResultManager")

  import dbConfig.profile.api._

  def getTestResultForSessionWrapper(sessionId: String): Option[TestResult] = {
    exec(getTestResultForSession(sessionId))
  }

  def getCommunityIdForTestSession(sessionId: String): Option[(String, Option[Long])] = {
    val result = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(r => (r.testSessionId, r.communityId)).result.headOption)
    result
  }

  def getOrganisationIdForTestSession(sessionId: String): Option[(String, Option[Long])] = {
    val result = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(r => (r.testSessionId, r.organizationId)).result.headOption)
    result
  }

  def getTestResultForSession(sessionId: String) = {
    PersistenceSchema.testResults.filter(_.testSessionId === sessionId).result.headOption
  }

  /**
   * Gets all running test results
   */
  def getRunningTestResults: List[TestResult] = {
    val results = exec(
      PersistenceSchema.testResults.filter(_.endTime.isEmpty).result.map(_.toList)
    )
    results
  }

  def updateForUpdatedSystem(id: Long, name: String) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.sutId === id} yield (t.sut)
    q1.update(Some(name))
  }

  def updateForUpdatedOrganisation(id: Long, name: String) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.organizationId === id} yield (t.organization)
    q1.update(Some(name))
  }

  def updateForUpdatedCommunity(id: Long, name: String) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.community)
    q1.update(Some(name))
  }

  def updateForUpdatedTestCase(id: Long, name: String) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testCaseId === id} yield (t.testCase)
    q1.update(Some(name))
  }

  def updateForUpdatedTestSuite(id: Long, name: String) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testSuiteId === id} yield (t.testSuite)
    q1.update(Some(name))
  }

  def updateForUpdatedDomain(id: Long, name: String) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.domainId === id} yield (t.domain)
    q1.update(Some(name))
  }

  def updateForUpdatedSpecification(id: Long, name: String) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.specificationId === id} yield (t.specification)
    q1.update(Some(name))
  }

  def updateForUpdatedActor(id: Long, name: String) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.actorId === id} yield (t.actor)
    q1.update(Some(name))
  }

  def updateForDeletedSystem(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.sutId === id} yield (t.sutId)
    q1.update(None)
  }

  def updateForDeletedOrganisation(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.organizationId === id} yield (t.organizationId)
    q1.update(None)
  }

  def updateForDeletedOrganisationByCommunityId(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.organizationId)
    q1.update(None)
  }

  def updateForDeletedCommunity(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.communityId)
    q1.update(None)
  }

  def updateForDeletedTestCase(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testCaseId === id} yield (t.testCaseId)
    q1.update(None)
  }

  def updateForDeletedTestSuite(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testSuiteId === id} yield (t.testSuiteId)
    q1.update(None)
  }

  def updateForDeletedDomain(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.domainId === id} yield (t.domainId)
    q1.update(None)
  }

  def updateForDeletedSpecification(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.specificationId === id} yield (t.specificationId)
    q1.update(None)
  }

  def updateForDeletedActor(id: Long) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.actorId === id} yield (t.actorId)
    q1.update(None)
  }

  def deleteObsoleteTestResultsForSystemWrapper(systemId: Long) = {
    exec(deleteObsoleteTestResultsForSystem(systemId).transactionally)
  }

  def deleteObsoleteTestResultsForCommunityWrapper(communityId: Long): Unit = {
    exec(deleteObsoleteTestResultsForCommunity(communityId).transactionally)
  }

  def deleteSessionDataFromFileSystem(testResult: TestResult) = {
    val path = ReportManager.getPathForTestSessionObj(testResult.sessionId, Some(testResult), true)
    try {
        FileUtils.deleteDirectory(path.toFile)
    } catch {
      case e:Exception => {
        Logger.warn("Unable to delete folder ["+path.toFile.getAbsolutePath+"] for obsolete session [" + testResult.sessionId + "]")
      }
    }
  }

  def deleteObsoleteTestResultsForSystem(systemId: Long) = {
    val query = PersistenceSchema.testResults
      .filter(x => x.sutId === systemId &&
        (x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
          x.communityId.isEmpty || x.organizationId.isEmpty ||
          x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty))
    // Delete filesystem data.
    (for {
      results <- query.result
      _ <- DBIO.seq(results.map(r => {
        deleteSessionDataFromFileSystem(r)
        DBIO.successful(())
      }): _*)
    } yield ()) andThen
    // Delete the DB records.
    query.delete
  }

  def deleteObsoleteTestResultsForCommunity(communityId: Long) = {
    val query = PersistenceSchema.testResults
      .filter(x => x.communityId === communityId &&
        (x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
          x.sutId.isEmpty || x.organizationId.isEmpty ||
          x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty))
    // Delete filesystem data.
    (for {
      results <- query.result
      _ <- DBIO.seq(results.map(r => {
        deleteSessionDataFromFileSystem(r)
        DBIO.successful(())
      }): _*)
    } yield()) andThen
    // Delete the DB records.
    query.delete
  }

  def deleteAllObsoleteTestResults(): Unit = {
    val query = PersistenceSchema.testResults
      .filter(x =>
        x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
          x.sutId.isEmpty || x.organizationId.isEmpty || x.communityId.isEmpty ||
          x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty)
    exec(
      (
        (for {
        results <- query.result
        _ <- DBIO.seq(results.map(r => {
          deleteSessionDataFromFileSystem(r)
          DBIO.successful(())
        }): _*)
      } yield()) andThen
        query.delete
      ).transactionally
    )
  }

}
