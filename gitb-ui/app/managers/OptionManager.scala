package managers

import javax.inject.{Inject, Singleton}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class OptionManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def deleteOptionByActor(actorId: Long) = {
    val action = (for {
      options <- PersistenceSchema.options.filter(_.actor === actorId).map(_.id).result
      _ <- DBIO.seq(options.map(optionId => delete(optionId)): _*)
    } yield ()).transactionally
    action
  }
  def delete(optionId: Long) = {
    PersistenceSchema.systemImplementsOptions.filter(_.optionId === optionId).delete andThen
    PersistenceSchema.testCaseCoversOptions.filter(_.option === optionId).delete andThen
    PersistenceSchema.options.filter(_.id === optionId).delete
  }


}
