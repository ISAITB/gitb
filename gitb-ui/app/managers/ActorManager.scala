package managers

import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

object ActorManager extends BaseManager {
  def logger = LoggerFactory.getLogger("ActorManager")

  /**
   * Checks if actor exists
   */
  def checkActorExistsInSpecification(actorId: String, specificationId: Long, otherThanId: Option[Long]): Boolean = {
    DB.withSession { implicit session =>
      var query = for {
        actor <- PersistenceSchema.actors
        specificationHasActors <- PersistenceSchema.specificationHasActors if specificationHasActors.actorId === actor.id
      } yield (actor, specificationHasActors)
      query = query
        .filter(_._1.actorId === actorId)
        .filter(_._2.specId === specificationId)
      if (otherThanId.isDefined) {
        query = query.filter(_._1.id =!= otherThanId.get)
      }
      val actor = query.firstOption
      actor.isDefined
    }
  }

  def deleteActorByDomain(domainId: Long)(implicit session: Session) = {
    val ids = PersistenceSchema.actors.filter(_.domain === domainId).map(_.id).list
    ids foreach { id =>
      delete(id)
    }
  }

  def deleteActor(actorId: Long) = {
    DB.withTransaction { implicit session =>
      delete(actorId)
    }
  }

  private def delete(actorId: Long)(implicit session: Session) = {
    PersistenceSchema.testCaseHasActors.filter(_.actor === actorId).delete
    PersistenceSchema.testSuiteHasActors.filter(_.actor === actorId).delete
    PersistenceSchema.systemImplementsActors.filter(_.actorId === actorId).delete
    PersistenceSchema.testCaseHasActors.filter(_.actor === actorId).delete
    PersistenceSchema.testSuiteHasActors.filter(_.actor === actorId).delete
    PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).delete
    PersistenceSchema.endpointSupportsTransactions.filter(_.actorId === actorId).delete
    EndPointManager.deleteEndPointByActor(actorId)
    OptionManager.deleteOptionByActor(actorId)
    PersistenceSchema.actors.filter(_.id === actorId).delete
  }

  def updateActor(id: Long, actorId: String, name: String, description: Option[String]) = {
    DB.withSession { implicit session =>
      val q1 = for {a <- PersistenceSchema.actors if a.id === id} yield (a.name)
      q1.update(name)

      val q2 = for {a <- PersistenceSchema.actors if a.id === id} yield (a.desc)
      q2.update(description)

      val q3 = for {a <- PersistenceSchema.actors if a.id === id} yield (a.actorId)
      q3.update(actorId)

    }
  }

}
