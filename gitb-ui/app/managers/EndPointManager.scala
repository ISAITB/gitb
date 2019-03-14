package managers

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class EndPointManager @Inject() (parameterManager: ParameterManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {
  def logger = LoggerFactory.getLogger("EndPointManager")

  import dbConfig.profile.api._

  def createEndpointWrapper(endpoint: models.Endpoints) = {
    exec(createEndpoint(endpoint).transactionally)
  }

  def createEndpoint(endpoint: models.Endpoints) = {
    PersistenceSchema.endpoints.returning(PersistenceSchema.endpoints.map(_.id)) += endpoint
  }

  def checkEndPointExistsForActor(endPointName: String, actorId: Long, otherThanId: Option[Long]): Boolean = {
    var endpointQuery = PersistenceSchema.endpoints
      .filter(_.name === endPointName)
      .filter(_.actor === actorId)
    if (otherThanId.isDefined) {
      endpointQuery = endpointQuery.filter(_.id =!= otherThanId.get)
    }
    val endpoint = endpointQuery.result.headOption
    exec(endpoint).isDefined
  }

  def deleteEndPointByActor(actorId: Long) = {
    val action = (for {
      ids <- PersistenceSchema.endpoints.filter(_.actor === actorId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => delete(id)): _*)
    } yield()).transactionally
    action
  }

  def deleteEndPoint(endPointId: Long) = {
    exec(delete(endPointId).transactionally)
  }

  def delete(endPointId: Long) = {
    (for {
      endPoint <- PersistenceSchema.endpoints.filter(_.id === endPointId).result.head
      _ <- PersistenceSchema.endpointSupportsTransactions.filter(_.endpoint === endPoint.name).delete
    } yield()) andThen
    parameterManager.deleteParameterByEndPoint(endPointId) andThen
    PersistenceSchema.configs.filter(_.endpoint === endPointId).delete andThen
    PersistenceSchema.endpoints.filter(_.id === endPointId).delete
  }

  def updateEndPointWrapper(endPointId: Long, name: String, description: Option[String]) =  {
    exec(updateEndPoint(endPointId, name, description).transactionally)
  }

  def updateEndPoint(endPointId: Long, name: String, description: Option[String]) =  {
    val q1 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.name)
    val q2 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.desc)
    q1.update(name) andThen
    q2.update(description)
  }

}
