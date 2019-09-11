package managers

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ParameterManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {
  def logger = LoggerFactory.getLogger("ParameterManager")

  import dbConfig.profile.api._

  def checkParameterExistsForEndpoint(parameterName: String, endpointId: Long, otherThanId: Option[Long]): Boolean = {
    var parameterQuery = PersistenceSchema.parameters
      .filter(_.name === parameterName)
      .filter(_.endpoint === endpointId)
    if (otherThanId.isDefined) {
      parameterQuery = parameterQuery.filter(_.id =!= otherThanId.get)
    }
    exec(parameterQuery.result.headOption).isDefined
  }

  def createParameterWrapper(parameter: models.Parameters) = {
    exec(createParameter(parameter).transactionally)
  }

  def createParameter(parameter: models.Parameters) = {
    PersistenceSchema.parameters.returning(PersistenceSchema.parameters.map(_.id)) += parameter
  }

  def deleteParameterByEndPoint(endPointId: Long) = {
    val action = for {
      ids <- PersistenceSchema.parameters.filter(_.endpoint === endPointId).map(_.id).result
      _ <- DBIO.seq(ids.map(id => delete(id)): _*)
    } yield()
    action
  }

  def deleteParameter(parameterId: Long) = {
    exec(delete(parameterId).transactionally)
  }

  def delete(parameterId: Long) = {
    PersistenceSchema.parameters.filter(_.id === parameterId).delete andThen
    PersistenceSchema.configs.filter(_.parameter === parameterId).delete
  }

  def updateParameterWrapper(parameterId: Long, name: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean) = {
    exec(updateParameter(parameterId, name, description, use, kind, adminOnly, notForTests).transactionally)
  }

  def getParameterById(parameterId: Long) = {
    exec(PersistenceSchema.parameters.filter(_.id === parameterId).result.headOption)
  }

  def updateParameter(parameterId: Long, name: String, description: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests: Boolean) = {
    val q = for {p <- PersistenceSchema.parameters if p.id === parameterId} yield (p.desc, p.use, p.kind, p.name, p.adminOnly, p.notForTests)
    q.update(description, use, kind, name, adminOnly, notForTests)
  }

}
