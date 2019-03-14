package managers

import javax.inject.{Inject, Singleton}
import models.Actors
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIOAction

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ActorManager @Inject() (testResultManager: TestResultManager, endPointManager: EndPointManager, optionManager: OptionManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("ActorManager")

  /**
   * Checks if actor exists
   */
  def checkActorExistsInSpecification(actorId: String, specificationId: Long, otherThanId: Option[Long]): Boolean = {
    var query = PersistenceSchema.actors
      .join(PersistenceSchema.specificationHasActors).on(_.id === _.actorId)
    query = query
      .filter(_._1.actorId === actorId)
      .filter(_._2.specId === specificationId)
    if (otherThanId.isDefined) {
      query = query.filter(_._1.id =!= otherThanId.get)
    }
    val actor = query.result.headOption
    exec(actor).isDefined
  }

  def deleteActorWrapper(actorId: Long) = {
    exec(deleteActor(actorId).transactionally)
  }

  def deleteActor(actorId: Long) = {
    delete(actorId)
  }

  private def delete(actorId: Long) = {
    testResultManager.updateForDeletedActor(actorId) andThen
    PersistenceSchema.testCaseHasActors.filter(_.actor === actorId).delete andThen
    PersistenceSchema.testSuiteHasActors.filter(_.actor === actorId).delete andThen
    PersistenceSchema.systemImplementsActors.filter(_.actorId === actorId).delete andThen
    PersistenceSchema.testCaseHasActors.filter(_.actor === actorId).delete andThen
    PersistenceSchema.testSuiteHasActors.filter(_.actor === actorId).delete andThen
    PersistenceSchema.specificationHasActors.filter(_.actorId === actorId).delete andThen
    PersistenceSchema.endpointSupportsTransactions.filter(_.actorId === actorId).delete andThen
    endPointManager.deleteEndPointByActor(actorId) andThen
    optionManager.deleteOptionByActor(actorId) andThen
    PersistenceSchema.conformanceResults.filter(_.actor === actorId).delete andThen
    PersistenceSchema.actors.filter(_.id === actorId).delete
  }

  def updateActorWrapper(id: Long, actorId: String, name: String, description: Option[String], default: Option[Boolean], displayOrder: Option[Short], specificationId: Long) = {
    exec(updateActor(id, actorId, name, description, default, displayOrder, specificationId).transactionally)
  }

  def updateActor(id: Long, actorId: String, name: String, description: Option[String], default: Option[Boolean], displayOrder: Option[Short], specificationId: Long) = {
    var defaultToSet: Option[Boolean] = null
    if (default.isEmpty) {
      defaultToSet = Some(false)
    } else {
      defaultToSet = default
    }
    (for  {
      _ <- {
        val q1 = for {a <- PersistenceSchema.actors if a.id === id} yield (a.name, a.desc, a.actorId, a.default, a.displayOrder)
        q1.update((name, description, actorId, defaultToSet, displayOrder))
      }
      _ <- {
        if (default.isDefined && default.get) {
          // Ensure no other default actors are defined.
          setOtherActorsAsNonDefault(id, specificationId)
        } else {
          DBIOAction.successful(())
        }
      }
    } yield()) andThen
    testResultManager.updateForUpdatedActor(id, name)
  }

  def getById(id: Long): Option[Actors] = {
    exec(PersistenceSchema.actors.filter(_.id === id).result.headOption)
  }

  def setOtherActorsAsNonDefault(defaultActorId: Long, specificationId: Long) = {
    val actions = (for {
      actorIds <- PersistenceSchema.specificationHasActors
        .filter(_.specId === specificationId)
        .map(e => e.actorId)
        .result
      _ <- DBIO.seq(actorIds.map(actorId =>
        if (actorId != defaultActorId) {
          val q = for (a <- PersistenceSchema.actors if a.id === actorId) yield a.default
          q.update(Some(false))
        } else {
          DBIOAction.successful(())
        }
      ): _*)
    } yield()).transactionally
    actions
  }

}
