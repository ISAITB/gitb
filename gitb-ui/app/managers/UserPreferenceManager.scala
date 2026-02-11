/*
 * Copyright (C) 2026 European Union
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

import models.{UserPreferenceBase, UserPreferenceDefaults, UserPreferences}
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserPreferenceManager @Inject() (dbConfigProvider: DatabaseConfigProvider)
                            (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getDefaultUserPreferences(communityId: Long): Future[UserPreferenceDefaults] = {
    DB.run {
      getDefaultUserPreferencesInternal(communityId)
    }
  }

  private def getDefaultUserPreferencesInternal(communityId: Long) = {
    PersistenceSchema.userPreferenceDefaults
      .filter(_.community === communityId)
      .result
      .headOption
      .map(x => x.getOrElse(UserPreferenceDefaults.createDefault(communityId)))
  }

  def initialiseUserPreferences(communityId: Long, userId: Long): DBIO[Unit] = {
    for {
      communityDefaults <- getDefaultUserPreferencesInternal(communityId)
      _ <- PersistenceSchema.userPreferences += UserPreferences.fromCommunityDefaults(userId, communityDefaults)
    } yield ()
  }

  def loadUserPreferencesMap(communityId: Long): Future[Map[Long, UserPreferences]] = {
    DB.run {
      PersistenceSchema.userPreferences
        .join(PersistenceSchema.users).on(_.user === _.id)
        .join(PersistenceSchema.organizations).on(_._2.organization === _.id)
        .filter(_._2.community === communityId)
        .map(_._1._1)
        .result
        .map { result =>
          result.map(pref => pref.user -> pref).toMap
        }
    }
  }

  def createPreferences(userId: Long, communityId: Long, preferences: Option[UserPreferences]): DBIO[Unit] = {
    if (preferences.isDefined) {
      (PersistenceSchema.userPreferences += preferences.get.copy(id = 0L, user = userId)).map(_ => ())
    } else {
      initialiseUserPreferences(communityId, userId)
    }
  }

  def deletePreferencesForUser(userId: Long): DBIO[Unit] = {
    PersistenceSchema.userPreferences.filter(_.user === userId).delete.map(_ => ())
  }

  def deletePreferencesForUsers(userIds: Iterable[Long]): DBIO[Unit] = {
    PersistenceSchema.userPreferences.filter(_.user inSet userIds).delete.map(_ => ())
  }

  def updatePreferencesInternal(userId: Long, preferences: UserPreferenceBase): DBIO[Unit] = {
    PersistenceSchema.userPreferences.filter(_.user === userId)
      .map(x => (x.menuCollapsed, x.statementsCollapsed, x.pageSize))
      .update((preferences.menuCollapsed, preferences.statementsCollapsed, preferences.pageSize))
      .map(_ => ())
  }

  def updatePreferenceForMenuCollapsed(userId: Long, value: Boolean): Future[Unit] = {
    val dbAction = PersistenceSchema.userPreferences.filter(_.user === userId).map(_.menuCollapsed).update(value)
    DB.run(dbAction.transactionally).map(_ => ())
  }

  def updatePreferenceForStatementsCollapsed(userId: Long, value: Boolean): Future[Unit] = {
    val dbAction = PersistenceSchema.userPreferences.filter(_.user === userId).map(_.statementsCollapsed).update(value)
    DB.run(dbAction.transactionally).map(_ => ())
  }

  def updatePreferenceForPageSize(userId: Long, value: Short): Future[Unit] = {
    val dbAction = PersistenceSchema.userPreferences.filter(_.user === userId).map(_.pageSize).update(value)
    DB.run(dbAction.transactionally).map(_ => ())
  }

  def updateUserPreferenceDefaults(communityId: Long, preferences: UserPreferenceBase, overrideExistingUserPreferences: Boolean): DBIO[Unit] = {
    for {
      _ <- PersistenceSchema.userPreferenceDefaults
        .filter(_.community === communityId)
        .map(x => (x.menuCollapsed, x.statementsCollapsed, x.pageSize))
        .update((preferences.menuCollapsed, preferences.statementsCollapsed, preferences.pageSize))
      _ <- {
        if (overrideExistingUserPreferences) {
          for {
            // Get the IDs to update (all preferences for users in the community).
            preferenceIds <- PersistenceSchema.userPreferences
              .join(PersistenceSchema.users).on(_.user === _.id)
              .join(PersistenceSchema.organizations).on(_._2.organization === _.id)
              .filter(_._2.community === communityId)
              .map(_._1._1.id)
              .result
            // Make the update.
            _ <- PersistenceSchema.userPreferences
              .filter(_.id inSet preferenceIds)
              .map(x => (x.menuCollapsed, x.statementsCollapsed, x.pageSize))
              .update((preferences.menuCollapsed, preferences.statementsCollapsed, preferences.pageSize))
          } yield ()
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

}
