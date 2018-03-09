package managers

import models.Specifications
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._
import utils.RepositoryUtils

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

object SpecificationManager extends BaseManager {
  def logger = LoggerFactory.getLogger("SpecificationManager")

  /**
   * Checks if domain exists
   */
  def checkSpecifiationExists(specId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.specifications.filter(_.id === specId).firstOption
        firstOption.isDefined
      }
    }
  }

  def deleteSpecificationByDomain(domainId: Long)(implicit session: Session) = {
    val ids = PersistenceSchema.specifications.filter(_.domain === domainId).map(_.id).list
    ids foreach { id =>
      delete(id)
    }
  }

  def deleteSpecification(specId: Long) = Future[Unit] {
    Future {
      DB.withTransaction { implicit session =>
        delete(specId)
      }
    }
  }

  def getSpecificationById(specId: Long): Specifications = {
    DB.withSession { implicit session =>
      val spec = PersistenceSchema.specifications.filter(_.id === specId).firstOption.get
      spec
    }
  }

  def delete(specId: Long)(implicit session: Session) = {
    PersistenceSchema.specificationHasActors.filter(_.specId === specId).delete
    val ids = PersistenceSchema.testSuites.filter(_.specification === specId).map(_.id).list
    ids foreach { id =>
      TestSuiteManager.undeployTestSuite(id)
    }
    RepositoryUtils.deleteSpecificationTestSuiteFolder(specId)
    PersistenceSchema.specifications.filter(_.id === specId).delete
  }

  def updateSpecification(specId: Long, sname: String, fname: String, urls: Option[String], diagram: Option[String], descr: Option[String], specificationType: Option[Short]) = {
    Future {
      DB.withSession { implicit session =>
        val q1 = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.shortname)
        q1.update(sname)

        val q2 = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.fullname)
        q2.update(fname)

        val q3 = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.urls)
        q3.update(urls)

        val q4 = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.diagram)
        q4.update(diagram)

        val q5 = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.description)
        q5.update(descr)

        val q6 = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.stype)
        q6.update(specificationType.get)
      }
    }
  }

}
