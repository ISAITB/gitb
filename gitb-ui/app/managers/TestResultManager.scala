package managers

import models._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.Logger

import scala.slick.driver.MySQLDriver.simple._

object TestResultManager extends BaseManager {
  def logger = LoggerFactory.getLogger("TestResultManager")

  def getTestResultForSessionWrapper(sessionId: String): Option[TestResult] = {
    DB.withSession { implicit session =>
      getTestResultForSession(sessionId)
    }
  }

  def getTestResultForSession(sessionId: String)(implicit session: Session): Option[TestResult] = {
    PersistenceSchema.testResults.filter(_.testSessionId === sessionId).firstOption
  }

  /**
   * Gets all running test results
   */
  def getRunningTestResults: List[TestResult] = {
    DB.withSession { implicit session =>
      val results = PersistenceSchema.testResults.filter(_.endTime.isEmpty).list
      results
    }
  }

  def updateForUpdatedSystem(id: Long, name: String)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.sutId === id} yield (t.sut)
    q1.update(Some(name))
  }

  def updateForUpdatedOrganisation(id: Long, name: String)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.organizationId === id} yield (t.organization)
    q1.update(Some(name))
  }

  def updateForUpdatedCommunity(id: Long, name: String)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.community)
    q1.update(Some(name))
  }

  def updateForUpdatedTestCase(id: Long, name: String)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testCaseId === id} yield (t.testCase)
    q1.update(Some(name))
  }

  def updateForUpdatedTestSuite(id: Long, name: String)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testSuiteId === id} yield (t.testSuite)
    q1.update(Some(name))
  }

  def updateForUpdatedDomain(id: Long, name: String)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.domainId === id} yield (t.domain)
    q1.update(Some(name))
  }

  def updateForUpdatedSpecification(id: Long, name: String)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.specificationId === id} yield (t.specification)
    q1.update(Some(name))
  }

  def updateForUpdatedActor(id: Long, name: String)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.actorId === id} yield (t.actor)
    q1.update(Some(name))
  }

  def updateForDeletedSystem(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.sutId === id} yield (t.sutId)
    q1.update(None)
  }

  def updateForDeletedOrganisation(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.organizationId === id} yield (t.organizationId)
    q1.update(None)
  }

  def updateForDeletedOrganisationByCommunityId(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.organizationId)
    q1.update(None)
  }

  def updateForDeletedCommunity(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.communityId)
    q1.update(None)
  }

  def updateForDeletedTestCase(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testCaseId === id} yield (t.testCaseId)
    q1.update(None)
  }

  def updateForDeletedTestSuite(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.testSuiteId === id} yield (t.testSuiteId)
    q1.update(None)
  }

  def updateForDeletedDomain(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.domainId === id} yield (t.domainId)
    q1.update(None)
  }

  def updateForDeletedSpecification(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.specificationId === id} yield (t.specificationId)
    q1.update(None)
  }

  def updateForDeletedActor(id: Long)(implicit session: Session) = {
    val q1 = for {t <- PersistenceSchema.testResults if t.actorId === id} yield (t.actorId)
    q1.update(None)
  }

  def deleteObsoleteTestResultsForSystemWrapper(systemId: Long): Unit = {
    DB.withTransaction { implicit session =>
      deleteObsoleteTestResultsForSystem(systemId)
    }
  }

  def deleteObsoleteTestResultsForCommunityWrapper(communityId: Long): Unit = {
    DB.withTransaction { implicit session =>
      deleteObsoleteTestResultsForCommunity(communityId)
    }
  }

  def deleteSessionDataFromFileSystem(testResult: TestResult)(implicit session: Session): Unit = {
    val path = ReportManager.getPathForTestSessionObj(testResult.sessionId, Some(testResult), true)
    try {
        FileUtils.deleteDirectory(path.toFile)
    } catch {
      case e:Exception => {
        Logger.warn("Unable to delete folder ["+path.toFile.getAbsolutePath+"] for obsolete session [" + testResult.sessionId + "]")
      }
    }
  }

  def deleteObsoleteTestResultsForSystem(systemId: Long)(implicit session: Session): Unit = {
    val query = PersistenceSchema.testResults
      .filter(x => x.sutId === systemId &&
        (x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
          x.communityId.isEmpty || x.organizationId.isEmpty ||
          x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty))
    val results = query.list
    results.foreach{ result =>
      deleteSessionDataFromFileSystem(result)
    }
    query.delete
  }

  def deleteObsoleteTestResultsForCommunity(communityId: Long)(implicit session: Session): Unit = {
    val query = PersistenceSchema.testResults
      .filter(x => x.communityId === communityId &&
        (x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
          x.sutId.isEmpty || x.organizationId.isEmpty ||
          x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty))
    val results = query.list
    results.foreach{ result =>
      deleteSessionDataFromFileSystem(result)
    }
    query.delete
  }

  def deleteAllObsoleteTestResults(): Unit = {
    DB.withTransaction { implicit session =>
      val query = PersistenceSchema.testResults
        .filter(x =>
          x.testSuiteId.isEmpty || x.testCaseId.isEmpty ||
            x.sutId.isEmpty || x.organizationId.isEmpty || x.communityId.isEmpty ||
            x.domainId.isEmpty || x.actorId.isEmpty || x.specificationId.isEmpty)
      val results = query.list
      results.foreach{ result =>
        deleteSessionDataFromFileSystem(result)
      }
      query.delete
    }
  }


}
