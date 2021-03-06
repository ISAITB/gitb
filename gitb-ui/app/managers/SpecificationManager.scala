package managers

import javax.inject.{Inject, Singleton}
import models.Specifications
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.ExecutionContext.Implicits.global

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

  def getSpecificationsById(specIds: List[Long]): List[Specifications] = {
    exec(PersistenceSchema.specifications.filter(_.id inSet specIds).result.map(_.toList))
  }

  def updateSpecificationInternal(specId: Long, sname: String, fname: String, descr: Option[String], hidden:Boolean): DBIO[_] = {
    val q = for {s <- PersistenceSchema.specifications if s.id === specId} yield (s.shortname, s.fullname, s.description, s.hidden)
    q.update(sname, fname, descr, hidden) andThen
      testResultManager.updateForUpdatedSpecification(specId, sname)
  }

  def updateSpecification(specId: Long, sname: String, fname: String, descr: Option[String], hidden:Boolean) = {
    exec(updateSpecificationInternal(specId, sname, fname, descr, hidden).transactionally)
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
