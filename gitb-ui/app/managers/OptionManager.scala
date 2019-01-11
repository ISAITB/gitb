package managers

import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.concurrent.ExecutionContext.Implicits.global

object OptionManager extends BaseManager {
  def logger = LoggerFactory.getLogger("OptionManager")

  import dbConfig.driver.api._

  def deleteOptionByActor(actorId: Long) = {
    val action = (for {
      options <- PersistenceSchema.options.filter(_.actor === actorId).map(_.id).result
      _ <- DBIO.seq(options.map(optionId => delete(optionId)): _*)
    } yield ()).transactionally
    action
  }

  def deleteOption(optionId: Long) = {
    exec(delete(optionId).transactionally)
  }

  def delete(optionId: Long) = {
    PersistenceSchema.systemImplementsOptions.filter(_.optionId === optionId).delete andThen
    PersistenceSchema.testCaseCoversOptions.filter(_.option === optionId).delete andThen
    PersistenceSchema.options.filter(_.id === optionId).delete
  }


}
