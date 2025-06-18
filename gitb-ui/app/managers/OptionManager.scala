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
