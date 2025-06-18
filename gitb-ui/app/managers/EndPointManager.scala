/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package managers

import javax.inject.{Inject, Singleton}
import models.{Endpoint, Endpoints}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EndPointManager @Inject() (parameterManager: ParameterManager,
                                 dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def createEndpointWrapper(endpoint: models.Endpoints): Future[Long] = {
    DB.run(createEndpoint(endpoint).transactionally)
  }

  def createEndpoint(endpoint: models.Endpoints): DBIO[Long] = {
    PersistenceSchema.endpoints.returning(PersistenceSchema.endpoints.map(_.id)) += endpoint
  }

  def checkEndPointExistsForActor(endPointName: String, actorId: Long, otherThanId: Option[Long]): Future[Boolean] = {
    var endpointQuery = PersistenceSchema.endpoints
      .filter(_.name === endPointName)
      .filter(_.actor === actorId)
    if (otherThanId.isDefined) {
      endpointQuery = endpointQuery.filter(_.id =!= otherThanId.get)
    }
    val endpoint = endpointQuery.result.headOption
    DB.run(endpoint).map(_.isDefined)
  }

  def deleteEndPointByActor(actorId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    val action = (for {
      ids <- PersistenceSchema.endpoints.filter(_.actor === actorId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => delete(id, onSuccessCalls)): _*)
    } yield()).transactionally
    action
  }

  def getById(endPointId: Long): Future[Endpoints] = {
    DB.run(PersistenceSchema.endpoints.filter(_.id === endPointId).result.head)
  }

  def getDomainIdsOfEndpoints(endpointIds: List[Long]): Future[Seq[Long]] = {
    DB.run(
      PersistenceSchema.endpoints
        .join(PersistenceSchema.actors).on(_.actor === _.id)
        .filter(_._1.id inSet endpointIds)
        .map(_._2.domain)
        .distinct
        .result
    )
  }

  def deleteEndPoint(endPointId: Long): Future[Unit] = {
    val onSuccessCalls = mutable.ListBuffer[() => _]()
    val dbAction = delete(endPointId, onSuccessCalls)
    DB.run(dbActionFinalisation(Some(onSuccessCalls), None, dbAction).transactionally).map(_ => ())
  }

  def delete(endPointId: Long, onSuccessCalls: mutable.ListBuffer[() => _]): DBIO[_] = {
    for {
      endPoint <- PersistenceSchema.endpoints.filter(_.id === endPointId).result.head
      _ <- PersistenceSchema.endpointSupportsTransactions.filter(_.endpoint === endPoint.name).delete
      _ <- parameterManager.deleteParameterByEndPoint(endPointId, onSuccessCalls)
      _ <- PersistenceSchema.endpoints.filter(_.id === endPointId).delete
    } yield()
  }

  def updateEndPointWrapper(endPointId: Long, name: String, description: Option[String]): Future[Unit] =  {
    DB.run(updateEndPoint(endPointId, name, description).transactionally).map(_ => ())
  }

  def updateEndPoint(endPointId: Long, name: String, description: Option[String]): DBIO[_] =  {
    PersistenceSchema.endpoints
      .filter(_.id === endPointId)
      .map(x => (x.name, x.desc))
      .update((name, description))
  }

  def getEndpointsForActor(actorId: Long): Future[List[Endpoint]] = {
    DB.run(
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
    ).map { result =>
      result._1.map(endpointInfo => {
        new Endpoint(endpointInfo._1.id, endpointInfo._1.name, endpointInfo._1.desc,
          Some(endpointInfo._2), // Actor
          Some(result._2.filter(_.endpoint == endpointInfo._1.id).toList) // Parameters
        )
      }).toList
    }
  }

  def getEndpoints(ids: Option[List[Long]]): Future[List[Endpoint]] = {
    DB.run(
      for {
        endpointsWithActors <- PersistenceSchema.endpoints
          .join(PersistenceSchema.actors).on(_.actor === _.id)
          .filterOpt(ids)((q, ids) => q._1.id inSet ids)
          .sortBy(_._1.name.asc)
          .result
        parameterMap <- {
          PersistenceSchema.parameters
            .filter(_.endpoint inSet endpointsWithActors.map(_._1.id))
            .sortBy(x => (x.displayOrder.asc, x.name.asc))
            .result
            .map { results =>
              val parameterMap = mutable.HashMap[Long, ListBuffer[models.Parameters]]()
              results.foreach { parameter =>
                val buffer = if (parameterMap.contains(parameter.endpoint)) {
                  parameterMap(parameter.endpoint)
                } else {
                  val endpointBuffer = new ListBuffer[models.Parameters]
                  parameterMap += (parameter.endpoint -> endpointBuffer)
                  endpointBuffer
                }
                buffer += parameter
              }
              parameterMap
            }
        }
        results <- {
          val buffer = new ListBuffer[Endpoint]
          endpointsWithActors.foreach { endpointWithActor =>
            val endpointParameters = parameterMap.get(endpointWithActor._1.id).map(_.toList).getOrElse(List())
            buffer += new Endpoint(endpointWithActor._1, endpointWithActor._2, endpointParameters)
          }
          DBIO.successful(buffer.toList)
        }
      } yield results
    )
  }

}
