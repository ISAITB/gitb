package managers

import javax.inject.{Inject, Singleton}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.ExecutionContext

@Singleton
class OptionManager @Inject() (dbConfigProvider: DatabaseConfigProvider)
                              (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def deleteOptionByActor(actorId: Long): DBIO[_] = {
    for {
      options <- PersistenceSchema.options.filter(_.actor === actorId).map(_.id).result
      _ <- DBIO.seq(options.map(optionId => delete(optionId)): _*)
    } yield ()
  }
  def delete(optionId: Long): DBIO[_] = {
    for {
      _ <- PersistenceSchema.systemImplementsOptions.filter(_.optionId === optionId).delete
      _ <- PersistenceSchema.testCaseCoversOptions.filter(_.option === optionId).delete
      _ <- PersistenceSchema.options.filter(_.id === optionId).delete
    } yield ()
  }

}
