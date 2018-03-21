package managers

import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

object EndPointManager extends BaseManager {
  def logger = LoggerFactory.getLogger("EndPointManager")

  def createEndpoint(endpoint: models.Endpoints) = {
    DB.withSession { implicit session =>
      PersistenceSchema.endpoints.returning(PersistenceSchema.endpoints.map(_.id)).insert(endpoint)
    }
  }

  def checkEndPointExistsForActor(endPointName: String, actorId: Long, otherThanId: Option[Long]): Boolean = {
    DB.withSession { implicit session =>
      var endpointQuery = PersistenceSchema.endpoints
        .filter(_.name === endPointName)
        .filter(_.actor === actorId)
      if (otherThanId.isDefined) {
        endpointQuery = endpointQuery.filter(_.id =!= otherThanId.get)
      }
      val endpoint = endpointQuery.firstOption
      endpoint.isDefined
    }
  }

  def deleteEndPointByActor(actorId: Long)(implicit session: Session) = {
    val ids = PersistenceSchema.endpoints.filter(_.actor === actorId).map(_.id).list
    ids foreach { id =>
      delete(id)
    }
  }

  def deleteEndPoint(endPointId: Long) = {
    DB.withTransaction { implicit session =>
      delete(endPointId)
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
    DB.withSession { implicit session =>
      val q1 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.name)
      q1.update(name)

      val q2 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.desc)
      q2.update(description)
    }
  }

}
