package managers

import managers.ConformanceManager._
import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

object OptionManager extends BaseManager {
  def logger = LoggerFactory.getLogger("OptionManager")

  def deleteOptionByActor(actorId: Long)(implicit session: Session) = {
    val ids = PersistenceSchema.options.filter(_.actor === actorId).map(_.id).list
    ids foreach { id =>
      delete(id)
    }
  }

  def deleteOption(optionId: Long) = Future[Unit] {
    Future {
      DB.withTransaction { implicit session =>
        delete(optionId)
      }
    }
  }

  def delete(optionId: Long)(implicit session: Session) = {
    PersistenceSchema.systemImplementsOptions.filter(_.optionId === optionId).delete
    PersistenceSchema.testCaseCoversOptions.filter(_.option === optionId).delete
    PersistenceSchema.options.filter(_.id === optionId).delete
  }


}
