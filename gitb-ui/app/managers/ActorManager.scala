package managers

import models.Actors
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

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

  def deleteActorWrapper(actorId: Long) = {
    DB.withTransaction { implicit session =>
      deleteActor(actorId)
    }
  }

  def deleteActor(actorId: Long)(implicit session: Session) = {
    delete(actorId)
  }

  private def delete(actorId: Long)(implicit session: Session) = {
    TestResultManager.updateForDeletedActor(actorId)
    PersistenceSchema.testCaseHasActors.filter(_.actor === actorId).delete
    PersistenceSchema.testSuiteHasActors.filter(_.actor === actorId).delete
    PersistenceSchema.systemImplementsActors.filter(_.actorId === actorId).delete
    PersistenceSchema.testCaseHasActors.filter(_.actor === actorId).delete
    PersistenceSchema.testSuiteHasActors.filter(_.actor === actorId).delete
    PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).delete
    PersistenceSchema.endpointSupportsTransactions.filter(_.actorId === actorId).delete
    EndPointManager.deleteEndPointByActor(actorId)
    OptionManager.deleteOptionByActor(actorId)
    PersistenceSchema.conformanceResults.filter(_.actor === actorId).delete
    PersistenceSchema.actors.filter(_.id === actorId).delete
  }

  def updateActorWrapper(id: Long, actorId: String, name: String, description: Option[String], default: Option[Boolean], displayOrder: Option[Short], specificationId: Long) = {
    DB.withTransaction { implicit session =>
      updateActor(id, actorId, name, description, default, displayOrder, specificationId)
    }
  }

  def updateActor(id: Long, actorId: String, name: String, description: Option[String], default: Option[Boolean], displayOrder: Option[Short], specificationId: Long)(implicit session: Session) = {
    var defaultToSet: Option[Boolean] = null
    if (default.isEmpty) {
      defaultToSet = Some(false)
    } else {
      defaultToSet = default
    }
    val q1 = for {a <- PersistenceSchema.actors if a.id === id} yield (a.name, a.desc, a.actorId, a.default, a.displayOrder)
    q1.update((name, description, actorId, defaultToSet, displayOrder))
    if (default.isDefined && default.get) {
      // Ensure no other default actors are defined.
      setOtherActorsAsNonDefault(id, specificationId)
    }
    TestResultManager.updateForUpdatedActor(id, name)
  }

  def getById(id: Long)(implicit session: Session): Option[Actors] = {
    PersistenceSchema.actors.filter(_.id === id).firstOption
  }

  def setOtherActorsAsNonDefault(defaultActorId: Long, specificationId: Long)(implicit session: Session): Unit = {
    val actorIds = PersistenceSchema.specificationHasActors
      .filter(_.specId === specificationId)
      .map(e => e.actorId)
      .list
    actorIds.foreach { actorId =>
      if (actorId != defaultActorId) {
        val q = for (a <- PersistenceSchema.actors if a.id === actorId) yield a.default
        q.update(Some(false))
      }
    }
  }

}
