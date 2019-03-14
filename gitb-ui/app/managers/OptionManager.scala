package managers

import javax.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class OptionManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {
  def logger = LoggerFactory.getLogger("OptionManager")

  import dbConfig.profile.api._

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
