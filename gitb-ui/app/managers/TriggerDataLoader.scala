package managers

import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}

@Singleton
class TriggerDataLoader @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getOrganisationUsingSystem(systemId: Long): Option[Long] = {
    exec(PersistenceSchema.systems.filter(_.id === systemId).map(_.owner).result.headOption)
  }

  def getIdsUsingTestSession(sessionId: String): (Option[Long], Option[Long], Option[Long], Option[Long]) = { // Organisation ID, System ID, Actor ID, Specification ID
    val result = exec(PersistenceSchema.testResults.filter(_.testSessionId === sessionId).map(x => (x.organizationId, x.sutId, x.actorId, x.specificationId)).result.headOption)
    if (result.isDefined) {
      result.get
    } else {
      (None, None, None, None)
    }
  }
}
