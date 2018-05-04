package managers

import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

object ParameterManager extends BaseManager {
  def logger = LoggerFactory.getLogger("ParameterManager")

  def checkParameterExistsForEndpoint(parameterName: String, endpointId: Long, otherThanId: Option[Long]): Boolean = {
    DB.withSession { implicit session =>
      var parameterQuery = PersistenceSchema.parameters
        .filter(_.name === parameterName)
        .filter(_.endpoint === endpointId)
      if (otherThanId.isDefined) {
        parameterQuery = parameterQuery.filter(_.id =!= otherThanId.get)
      }
      val parameter = parameterQuery.firstOption
      parameter.isDefined
    }
  }

  def createParameterWrapper(parameter: models.Parameters) = {
    DB.withTransaction { implicit session =>
      createParameter(parameter)
    }
  }

  def createParameter(parameter: models.Parameters)(implicit session: Session) = {
    PersistenceSchema.parameters.returning(PersistenceSchema.parameters.map(_.id)).insert(parameter)
  }

  def deleteParameterByEndPoint(endPointId: Long)(implicit session: Session) = {
    val ids = PersistenceSchema.parameters.filter(_.endpoint === endPointId).map(_.id).list
    ids foreach { id =>
      delete(id)
    }
  }

  def deleteParameter(parameterId: Long) = {
    DB.withTransaction { implicit session =>
      delete(parameterId)
    }
  }

  def delete(parameterId: Long)(implicit session: Session) = {
    PersistenceSchema.parameters.filter(_.id === parameterId).delete
    PersistenceSchema.configs.filter(_.parameter === parameterId).delete
  }

  def updateParameterWrapper(parameterId: Long, name: String, description: Option[String], use: String, kind: String) = {
    DB.withTransaction { implicit session =>
      updateParameter(parameterId, name, description, use, kind)
    }
  }

  def updateParameter(parameterId: Long, name: String, description: Option[String], use: String, kind: String)(implicit session: Session) = {
    val q1 = for {p <- PersistenceSchema.parameters if p.id === parameterId} yield (p.desc)
    q1.update(description)

    val q2 = for {p <- PersistenceSchema.parameters if p.id === parameterId} yield (p.use)
    q2.update(use)

    val q3 = for {p <- PersistenceSchema.parameters if p.id === parameterId} yield (p.kind)
    q3.update(kind)

    val q4 = for {p <- PersistenceSchema.parameters if p.id === parameterId} yield (p.name)
    q4.update(name)
  }

}
