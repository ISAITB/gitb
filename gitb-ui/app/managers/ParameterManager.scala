package managers

import managers.ConformanceManager._
import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

object ParameterManager extends BaseManager {
  def logger = LoggerFactory.getLogger("ParameterManager")

  def deleteParameterByEndPoint(endPointId: Long)(implicit session: Session) = {
    val ids = PersistenceSchema.parameters.filter(_.endpoint === endPointId).map(_.id).list
    ids foreach { id =>
      delete(id)
    }
  }

  def deleteParameter(parameterId: Long) = Future[Unit] {
    Future {
      DB.withTransaction { implicit session =>
        delete(parameterId)
      }
    }
  }

  def delete(parameterId: Long)(implicit session: Session) = {
    PersistenceSchema.parameters.filter(_.id === parameterId).delete
    PersistenceSchema.configs.filter(_.parameter === parameterId).delete
  }

}
