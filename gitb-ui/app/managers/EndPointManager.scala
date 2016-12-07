package managers

import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

object EndPointManager extends BaseManager {
  def logger = LoggerFactory.getLogger("EndPointManager")

  /**
   * Checks if actor exists
   */
  def checkEndPointExists(endPointId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.endpoints.filter(_.id === endPointId).firstOption
        firstOption.isDefined
      }
    }
  }

  def deleteEndPointByActor(actorId: Long)(implicit session: Session) = {
    val ids = PersistenceSchema.endpoints.filter(_.actor === actorId).map(_.id).list
    ids foreach { id =>
      delete(id)
    }
  }

  def deleteEndPoint(endPointId: Long) = Future[Unit] {
    Future {
      DB.withTransaction { implicit session =>
        delete(endPointId)
      }
    }
  }

  def delete(endPointId: Long)(implicit session: Session) = {
    val endPoint = PersistenceSchema.endpoints.filter(_.id === endPointId).firstOption.get
    ParameterManager.deleteParameterByEndPoint(endPointId)
    PersistenceSchema.endpointSupportsTransactions.filter(_.endpoint === endPoint.name).delete
    PersistenceSchema.configs.filter(_.endpoint === endPointId).delete
    PersistenceSchema.endpoints.filter(_.id === endPointId).delete
  }

  def updateEndPoint(endPointId: Long, name: String, description: Option[String]) =  {
    Future {
      DB.withSession { implicit session =>
        val q1 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.name)
        q1.update(name)

        val q2 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.desc)
        q2.update(description)
      }
    }
  }

}
