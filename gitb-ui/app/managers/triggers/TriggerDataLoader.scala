/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
