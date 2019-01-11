package managers

import models.Specifications
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import utils.RepositoryUtils

import scala.concurrent.ExecutionContext.Implicits.global

object SpecificationManager extends BaseManager {
  def logger = LoggerFactory.getLogger("SpecificationManager")

  import dbConfig.driver.api._

  /**
   * Checks if domain exists
   */
  def checkSpecifiationExists(specId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.specifications.filter(_.id === specId).result.headOption)
    firstOption.isDefined
  }

  def deleteSpecificationByDomain(domainId: Long) = {
    val action = (for {
      ids <- PersistenceSchema.specifications.filter(_.domain === domainId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => delete(id)): _*)
    } yield ()).transactionally
    action
  }

  def deleteSpecification(specId: Long) = {
    exec(delete(specId).transactionally)
  }

  def getSpecificationById(specId: Long): Specifications = {
    val spec = exec(PersistenceSchema.specifications.filter(_.id === specId).result.head)
    spec
  }

  def delete(specId: Long) = {
    TestResultManager.updateForDeletedSpecification(specId) andThen
    // Delete also actors from the domain (they are now linked only to specifications
    (for {
      actorIds <- PersistenceSchema.specificationHasActors.filter(_.specId === specId).map(_.actorId).result
      _ <- DBIO.seq(actorIds.map(id => ActorManager.deleteActor(id)): _*)
    } yield ()) andThen
    PersistenceSchema.specificationHasActors.filter(_.specId === specId).delete andThen
    (for {
      ids <- PersistenceSchema.testSuites.filter(_.specification === specId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => TestSuiteManager.undeployTestSuite(id)): _*)
    } yield ()) andThen
    PersistenceSchema.conformanceResults.filter(_.spec === specId).delete andThen
    {
      RepositoryUtils.deleteSpecificationTestSuiteFolder(specId)
      DBIO.successful(())
    } andThen
    PersistenceSchema.specifications.filter(_.id === specId).delete
  }

  def updateSpecification(specId: Long, sname: String, fname: String, urls: Option[String], diagram: Option[String], descr: Option[String], specificationType: Option[Short]) = {
    val q = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.shortname, s.fullname, s.urls, s.diagram, s.description, s.stype)
    exec(
      (
        q.update(sname, fname, urls, diagram, descr, specificationType.get) andThen
        TestResultManager.updateForUpdatedSpecification(specId, sname)
      ).transactionally
    )
  }

  def getSpecificationOfActor(actorId: Long) = {
    val query = PersistenceSchema.specifications
        .join(PersistenceSchema.specificationHasActors).on(_.id === _.specId)
    exec(query.filter(_._2.actorId === actorId).result.head)._1
  }

}
