package managers

import javax.inject.{Inject, Singleton}
import models.Specifications
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

@Singleton
class SpecificationManager @Inject() (actorManager: ActorManager, testResultManager: TestResultManager, testSuiteManager: TestSuiteManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {
  def logger = LoggerFactory.getLogger("SpecificationManager")

  import dbConfig.profile.api._

  /**
   * Checks if domain exists
   */
  def checkSpecifiationExists(specId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.specifications.filter(_.id === specId).result.headOption)
    firstOption.isDefined
  }

  def getSpecificationById(specId: Long): Specifications = {
    val spec = exec(PersistenceSchema.specifications.filter(_.id === specId).result.head)
    spec
  }

  def updateSpecification(specId: Long, sname: String, fname: String, urls: Option[String], diagram: Option[String], descr: Option[String], specificationType: Option[Short]) = {
    val q = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.shortname, s.fullname, s.urls, s.diagram, s.description, s.stype)
    exec(
      (
        q.update(sname, fname, urls, diagram, descr, specificationType.get) andThen
        testResultManager.updateForUpdatedSpecification(specId, sname)
      ).transactionally
    )
  }

  def getSpecificationIdOfActor(actorId: Long) = {
    exec(PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).result.head)._1
  }

  def getSpecificationOfActor(actorId: Long) = {
    val query = PersistenceSchema.specifications
        .join(PersistenceSchema.specificationHasActors).on(_.id === _.specId)
    exec(query.filter(_._2.actorId === actorId).result.head)._1
  }

}
