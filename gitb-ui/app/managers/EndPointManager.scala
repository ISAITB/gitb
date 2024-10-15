package managers

import javax.inject.{Inject, Singleton}
import models.{Endpoint, Endpoints}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class EndPointManager @Inject() (parameterManager: ParameterManager,
                                 dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def createEndpointWrapper(endpoint: models.Endpoints): Long = {
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

  def deleteEndPointByActor(actorId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
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

  def deleteEndPoint(endPointId: Long): Unit = {
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

  def updateEndPointWrapper(endPointId: Long, name: String, description: Option[String]): Unit =  {
    exec(updateEndPoint(endPointId, name, description).transactionally)
  }

  def updateEndPoint(endPointId: Long, name: String, description: Option[String]): DBIO[_] =  {
    val q1 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.name)
    val q2 = for {e <- PersistenceSchema.endpoints if e.id === endPointId} yield (e.desc)
    q1.update(name) andThen
    q2.update(description)
  }

  def getEndpointsCaseForActor(actorId: Long): List[Endpoints] = {
    exec(PersistenceSchema.endpoints.filter(_.actor === actorId).sortBy(_.name.asc).result).toList
  }

  def getEndpointsForActor(actorId: Long): List[Endpoint] = {
    val result = exec(
      for {
        endpoints <- PersistenceSchema.endpoints
          .join(PersistenceSchema.actors).on(_.actor === _.id)
          .filter(_._1.actor === actorId)
          .sortBy(_._1.name.asc)
          .map(x => (x._1, x._2)) // (Endpoint, Actor)
          .result
        parameters <- PersistenceSchema.parameters
          .join(PersistenceSchema.endpoints).on(_.endpoint === _.id)
          .filter(_._1.endpoint inSet endpoints.map(_._1.id).toSet)
          .sortBy(x => (x._1.endpoint.asc, x._1.displayOrder.asc))
          .map(_._1)
          .result
      } yield (endpoints, parameters)
    )
    result._1.map(endpointInfo => {
      new Endpoint(endpointInfo._1.id, endpointInfo._1.name, endpointInfo._1.desc,
        Some(endpointInfo._2), // Actor
        Some(result._2.filter(_.endpoint == endpointInfo._1.id).toList) // Parameters
      )
    }).toList
  }

  def getEndpoints(ids: Option[List[Long]]): List[Endpoint] = {
    val endpoints = new ListBuffer[Endpoint]()
    val q = ids match {
      case Some(list) => PersistenceSchema.endpoints.filter(_.id inSet list)
      case None => PersistenceSchema.endpoints
    }
    exec(q.sortBy(_.name.asc).result).map { caseObject =>
      val actor = exec(PersistenceSchema.actors.filter(_.id === caseObject.actor).result.head)
      val parameters = exec(PersistenceSchema.parameters.filter(_.endpoint === caseObject.id).sortBy(x => (x.displayOrder.asc, x.name.asc)).result.map(_.toList))
      endpoints += new Endpoint(caseObject, actor, parameters)
    }
    endpoints.toList
  }

}
