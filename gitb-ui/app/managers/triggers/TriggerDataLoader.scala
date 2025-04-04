package managers.triggers

import managers.BaseManager
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile.api._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TriggerDataLoader @Inject() (dbConfigProvider: DatabaseConfigProvider)
                                  (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  def getOrganisationUsingSystem(systemId: Long): Future[Option[Long]] = {
    DB.run(PersistenceSchema.systems.filter(_.id === systemId).map(_.owner).result.headOption)
  }

  def getIdsUsingTestSession(sessionId: String): Future[(Option[Long], Option[Long], Option[Long], Option[Long])] = { // Organisation ID, System ID, Actor ID, Specification ID
    DB.run(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(x => (x.organizationId, x.sutId, x.actorId, x.specificationId)).result.headOption).map { result =>
      if (result.isDefined) {
        result.get
      } else {
        (None, None, None, None)
      }
    }
  }
}
