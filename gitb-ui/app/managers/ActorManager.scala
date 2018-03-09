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
  def checkActorExists(actorId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.actors.filter(_.id === actorId).firstOption
        firstOption.isDefined
      }
    }
  }

  def deleteActorByDomain(domainId: Long)(implicit session: Session) = {
    val ids = PersistenceSchema.actors.filter(_.domain === domainId).map(_.id).list
    ids foreach { id =>
      delete(id)
    }
  }

  def deleteActor(actorId: Long) = Future[Unit] {
    Future {
      DB.withTransaction { implicit session =>
        delete(actorId)
      }
    }
  }

  def delete(actorId: Long)(implicit session: Session) = {
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

  def updateActor(actorId: Long, shortName: String, fullName: String, description: Option[String]) = {
    Future {
      DB.withSession { implicit session =>
        val q1 = for {a <- PersistenceSchema.actors if a.id === actorId} yield (a.actorId)
        q1.update(shortName)

        val q2 = for {a <- PersistenceSchema.actors if a.id === actorId} yield (a.name)
        q2.update(shortName)

        val q3 = for {a <- PersistenceSchema.actors if a.id === actorId} yield (a.desc)
        q3.update(description)
      }
    }
  }

}
