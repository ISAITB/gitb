package managers

import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

object TestResultManager extends BaseManager {
  def logger = LoggerFactory.getLogger("TestResultManager")

  def getTestResultForSession(sessionId: String): Option[TestResult] = {
    DB.withSession { implicit session =>
      PersistenceSchema.testResults.filter(_.testSessionId === sessionId).firstOption
    }
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

  def updateForUpdatedSystem(id: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.sutId === id} yield (t.sut)
      q1.update(Some(name))
    }
  }

  def updateForUpdatedOrganisation(id: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.organizationId === id} yield (t.organization)
      q1.update(Some(name))
    }
  }

  def updateForUpdatedCommunity(id: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.community)
      q1.update(Some(name))
    }
  }

  def updateForUpdatedTestCase(id: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.testCaseId === id} yield (t.testCase)
      q1.update(Some(name))
    }
  }

  def updateForUpdatedTestSuite(id: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.testSuiteId === id} yield (t.testSuite)
      q1.update(Some(name))
    }
  }

  def updateForUpdatedDomain(id: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.domainId === id} yield (t.domain)
      q1.update(Some(name))
    }
  }

  def updateForUpdatedSpecification(id: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.specificationId === id} yield (t.specification)
      q1.update(Some(name))
    }
  }

  def updateForUpdatedActor(id: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.actorId === id} yield (t.actor)
      q1.update(Some(name))
    }
  }

  def updateForDeletedSystem(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.sutId === id} yield (t.sutId)
      q1.update(None)
    }
  }

  def updateForDeletedOrganisation(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.organizationId === id} yield (t.organizationId)
      q1.update(None)
    }
  }

  def updateForDeletedOrganisationByCommunityId(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.organizationId)
      q1.update(None)
    }
  }

  def updateForDeletedCommunity(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.communityId === id} yield (t.communityId)
      q1.update(None)
    }
  }

  def updateForDeletedTestCase(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.testCaseId === id} yield (t.testCaseId)
      q1.update(None)
    }
  }

  def updateForDeletedTestSuite(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.testSuiteId === id} yield (t.testSuiteId)
      q1.update(None)
    }
  }

  def updateForDeletedDomain(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.domainId === id} yield (t.domainId)
      q1.update(None)
    }
  }

  def updateForDeletedSpecification(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.specificationId === id} yield (t.specificationId)
      q1.update(None)
    }
  }

  def updateForDeletedActor(id: Long) = {
    DB.withTransaction { implicit session =>
      val q1 = for {t <- PersistenceSchema.testResults if t.actorId === id} yield (t.actorId)
      q1.update(None)
    }
  }

}
