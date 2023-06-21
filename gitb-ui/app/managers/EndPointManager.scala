package managers

import javax.inject.{Inject, Singleton}
import models.Endpoints
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class EndPointManager @Inject() (parameterManager: ParameterManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def createEndpointWrapper(endpoint: models.Endpoints) = {
    exec(createEndpoint(endpoint).transactionally)
  }

  def createEndpoint(endpoint: models.Endpoints): DBIO[Long] = {
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

  def deleteEndPointByActor(actorId: Long, onSuccessCalls: mutable.ListBuffer[() => _]) = {
    val action = (for {
      ids <- PersistenceSchema.endpoints.filter(_.actor === actorId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => delete(id, onSuccessCalls)): _*)
    } yield()).transactionally
    action
  }

  def getById(endPointId: Long): Endpoints = {
    val endpoint = exec(PersistenceSchema.endpoints.filter(_.id === endPointId).result.head)
    endpoint
  }

  def deleteEndPoint(endPointId: Long) = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = delete(endPointId, onSuccessCalls)
    exec(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally)
  }

  def delete(endPointId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    (for {
      endPoint <- PersistenceSchema.endpoints.filter(_.id === endPointId).result.head
      _ <- PersistenceSchema.endpointSupportsTransactions.filter(_.endpoint === endPoint.name).delete
    } yield()) andThen
    parameterManager.deleteParameterByEndPoint(endPointId, onSuccessCalls) andThen
    PersistenceSchema.endpoints.filter(_.id === endPointId).delete
  }

  def updateEndPointWrapper(endPointId: Long, name: String, description: Option[String]) =  {
    exec(updateEndPoint(endPointId, name, description).transactionally)
  }

  def updateEndPoint(endPointId: Long, name: String, description: Option[String]): DBIO[_] =  {
    val q1 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.name)
    val q2 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.desc)
    q1.update(name) andThen
    q2.update(description)
  }

}
