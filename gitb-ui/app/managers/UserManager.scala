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

import config.Configurations
import models.Enums.UserRole
import models.Enums.UserRole._
import models._
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider
import utils.CryptoUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by VWYNGAET on 25/10/2016.
 */
@Singleton
class UserManager @Inject() (accountManager: AccountManager,
                             userPreferenceManager: UserPreferenceManager,
                             dbConfigProvider: DatabaseConfigProvider)
                            (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def getSystemAdministrators(page: Long, limit: Long): Future[SearchResult[Users]] = {
    val queryBuilder = (forCount: Boolean) => {
      var baseQuery = PersistenceSchema.users.filter(_.role === UserRole.SystemAdmin.id.toShort)
      if (!forCount) {
        baseQuery = baseQuery.sortBy(_.name.asc)
      }
      baseQuery
    }
    DB.run(
      for {
        results <- queryBuilder(false).drop((page - 1) * limit).take(limit).result
        resultCount <- queryBuilder(true).size.result
      } yield SearchResult(results, resultCount)
    )
  }

  /**
    * Gets all community administrators of the given community
    */
  def getCommunityAdministrators(communityId:Long, page: Long, limit: Long): Future[SearchResult[Users]] = {
    val queryBuilder = (forCount: Boolean) => {
      var baseQuery = PersistenceSchema.users
        .join(PersistenceSchema.organizations).on(_.organization === _.id)
        .filter(_._2.community === communityId)
        .map(_._1)
      if (!forCount) {
        baseQuery = baseQuery.sortBy(_.name.asc)
      }
      baseQuery
    }
    DB.run(
      for {
        results <- queryBuilder(false).drop((page - 1) * limit).take(limit).result
        resultCount <- queryBuilder(true).size.result
      } yield SearchResult(results, resultCount)
    )
  }

  def searchUsersByOrganization(orgId: Long, page: Long, limit: Long, role: Option[UserRole] = None): Future[SearchResult[Users]] = {
    val queryBuilder = (forCount: Boolean) => {
      var baseQuery = PersistenceSchema.users
        .filter(_.organization === orgId)
        .filterOpt(role)((q, r) => q.role === r.id.toShort)
        .filterIf(role.isEmpty)(_.role inSet Set(UserRole.VendorUser.id.toShort, UserRole.VendorAdmin.id.toShort))
      if (!forCount) {
        baseQuery = baseQuery.sortBy(x => (x.role.asc, x.name.asc))
      }
      baseQuery
    }
    DB.run(
      for {
        results <- queryBuilder(false).drop((page - 1) * limit).take(limit).result
        resultCount <- queryBuilder(true).size.result
      } yield SearchResult(results, resultCount)
    )
  }

  /**
   * Gets all users of specified organization
   */
  def getUsersByOrganization(orgId: Long, role: Option[UserRole] = None): Future[List[Users]] = {
    DB.run(PersistenceSchema.users
      .filter(_.organization === orgId)
      .filterOpt(role)((q, r) => q.role === r.id.toShort)
      .filterIf(role.isEmpty)(_.role inSet Set(UserRole.VendorUser.id.toShort, UserRole.VendorAdmin.id.toShort))
      .sortBy(_.name.asc)
      .result.map(_.toList))
  }

  def getById(userId: Long): Future[Users] = {
    DB.run(PersistenceSchema.users.filter(_.id === userId).result.head)
  }

  /**
   * Gets user with specified id
   */
  def getUserById(userId: Long): Future[Option[User]] = {
    DB.run(
      for {
        user <- PersistenceSchema.users.filter(_.id === userId).result.headOption
        organisation <- if (user.isDefined) {
          PersistenceSchema.organizations.filter(_.id === user.get.organization).result.headOption
        } else {
          DBIO.successful(None)
        }
      } yield (user, organisation)
    ).map { result =>
      if (result._1.isDefined && result._2.isDefined) {
        Some(new User(result._1.get, result._2.get))
      } else {
        None
      }
    }
  }

  /**
   * Gets user with specified id
   */
  def getUserByIdAsync(userId: Long): Future[Option[User]] = {
    val result = DB.run(for {
      user <- PersistenceSchema.users.filter(_.id === userId).result.headOption
      organisation <- if (user.isDefined) {
        PersistenceSchema.organizations.filter(_.id === user.get.organization).result.headOption
      } else {
        DBIO.successful(None)
      }
    } yield (user, organisation))
    result.map { result =>
      if (result._1.isDefined && result._2.isDefined) {
        Some(new User(result._1.get, result._2.get))
      } else {
        None
      }
    }
  }

  /**
   * Gets user with specified id in the same organisation as the given user
   */
  def getUserByIdInSameOrganisationAsUser(userId: Long, referenceUserId: Long): Future[Option[User]] = {
    DB.run(for {
      referenceOrganisationId <- PersistenceSchema.users.filter(_.id === referenceUserId).map(_.organization).result.headOption
      user <- if (referenceOrganisationId.isDefined) {
        PersistenceSchema.users
          .filter(_.organization === referenceOrganisationId.get)
          .filter(_.id === userId)
          .result
          .headOption
      } else {
        DBIO.successful(None)
      }
    } yield user).map(x => x.map(new User(_)))
  }

  /**
   * Updates system admin profile of given user
   */
  def updateSystemAdminProfile(userId: Long, name: String, password: Option[String]): Future[Unit] = {
    accountManager.checkUserRole(userId, UserRole.SystemAdmin).map { userExists =>
      if (userExists) {
        var action: DBIO[_] = null
        if (password.isDefined) {
          val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name, u.password, u.onetimePassword)
          action = q.update(name, CryptoUtil.hashPassword(String.valueOf(password.get.trim)), true)
        } else {
          val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
          action = q.update(name)
        }
        DB.run(action.transactionally)
      }
    }
  }

  /**
   * Updates community admin profile of given user
   */
  def updateCommunityAdminProfile(userId: Long, name: String, password: Option[String]): Future[Unit] = {
    accountManager.checkUserRole(userId, UserRole.CommunityAdmin).map { userExists =>
      if (userExists) {
        var action: DBIO[_] = null
        if (password.isDefined) {
          val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name, u.password, u.onetimePassword)
          action = q.update(name, CryptoUtil.hashPassword(password.get.trim), true)
        } else {
          val q = for {u <- PersistenceSchema.users if u.id === userId} yield u.name
          action = q.update(name)
        }
        DB.run(action.transactionally)
      }
    }
  }

  /**
   * Updates user profile of given user
   */
  def updateUserProfile(userId: Long, name: Option[String], roleId: Short, password: Option[String]): Future[Unit] = {
    val action = for {
      user <- PersistenceSchema.users.filter(_.id === userId).result.headOption
      _ <- {
        if (user.isDefined) {
          var action: DBIO[_] = null
          if (Configurations.AUTHENTICATION_SSO_ENABLED) {
            val q = for {u <- PersistenceSchema.users if u.id === userId} yield u.role
            action = q.update(roleId)
          } else {
            if (password.isDefined) {
              val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name, u.role, u.password, u.onetimePassword)
              action = q.update(name.get, roleId, CryptoUtil.hashPassword(password.get.trim), true)
            } else {
              val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name, u.role)
              action = q.update(name.get, roleId)
            }
          }
          action
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
    DB.run(action.transactionally)
  }

  private [managers] def createUserInternal(user: Users, preferences: Option[UserPreferences], communityId: Long): DBIO[Long] = {
    for {
      userId <- PersistenceSchema.insertUser += user
      _ <- userPreferenceManager.createPreferences(userId, communityId, preferences)
    } yield userId
  }

  private [managers] def updateUserInternal(userId: Long, name: String, password: String, oneTimePassword: Boolean, role: Option[Short], preferences: Option[UserPreferences]): DBIO[Unit] = {
    for {
      _ <- {
        if (role.isDefined) {
          PersistenceSchema.users
            .filter(_.id === userId)
            .map(x => (x.name, x.password, x.onetimePassword, x.role))
            .update((name, password, oneTimePassword, role.get))
        } else {
          PersistenceSchema.users
            .filter(_.id === userId)
            .map(x => (x.name, x.password, x.onetimePassword))
            .update((name, password, oneTimePassword))
        }
      }
      _ <- {
        if (preferences.isDefined) {
          userPreferenceManager.updatePreferencesInternal(userId, preferences.get)
        } else {
          DBIO.successful(())
        }
      }
    } yield ()
  }

  /**
   * Create system admin
   */
  def createAdmin(user: Users, communityId: Long): Future[Unit] = {
    val dbAction = for {
      organisationId <- PersistenceSchema.organizations
        .filter(_.community === communityId)
        .filter(_.adminOrganization === true)
        .map(_.id)
        .result
        .head
      userId <- PersistenceSchema.insertUser += user.withOrganizationId(organisationId)
      _ <- userPreferenceManager.initialiseUserPreferences(communityId, userId)
    } yield ()
    DB.run(dbAction.transactionally)
  }

  /**
   * Creates new user
   */
  def createUser(user: Users, orgId: Long): Future[Long] = {
    val action = for {
      // Make sure the organisation exists and retrieve its communityID.
      communityId <- PersistenceSchema.organizations
        .filter(_.id =!= Constants.DefaultOrganizationId)
        .filter(_.id === orgId)
        .map(_.community)
        .result
        .headOption
        .map { result =>
          if (result.isEmpty) {
            throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
          } else {
            result.get
          }
        }
      // Add the user.
      newUserId <- PersistenceSchema.insertUser += user.withOrganizationId(orgId)
      // Initialise the user preferences.
      _ <- userPreferenceManager.initialiseUserPreferences(communityId, newUserId)
    } yield newUserId
    DB.run(action.transactionally)
  }

  /**
   * Deletes user
   */
  def deleteUser(userId: Long): Future[Unit] = {
    DB.run {
      {
        for {
          _ <- userPreferenceManager.deletePreferencesForUser(userId)
          _ <- PersistenceSchema.users.filter(_.id === userId).delete
        } yield ()
      }.transactionally
    }
  }

  def deleteUsersByUidExceptTestBedAdmin(ssoUid: String, ssoEmail: String): Future[Unit] = {
    val dbAction = for {
      // Collect active role IDs
      activeUserIds <- PersistenceSchema.users
        .filter(_.ssoUid === ssoUid)
        .filter(_.role =!= Enums.UserRole.SystemAdmin.id.toShort)
        .map(_.id)
        .result
      // Collect inactive roles
      inactiveUserIds <- PersistenceSchema.users
        .filter(_.ssoEmail.isDefined)
        .filter(_.ssoEmail.toLowerCase === ssoEmail.toLowerCase)
        .filter(_.role =!= Enums.UserRole.SystemAdmin.id.toShort)
        .map(_.id)
        .result
      userIds <- DBIO.successful(activeUserIds.toSet ++ inactiveUserIds.toSet)
      // Delete preferences.
      _ <- userPreferenceManager.deletePreferencesForUsers(userIds)
      // Delete user entries.
      _ <- PersistenceSchema.users.filter(_.id inSet userIds).delete
    } yield ()
    DB.run(dbAction.transactionally)
  }

  /**
   * Deletes user
   */
  def deleteUserExceptTestBedAdmin(userId: Long): Future[Unit] = {
    DB.run {
      {
        for {
          userIds <- PersistenceSchema.users
            .filter(_.id === userId)
            .filter(_.role =!= Enums.UserRole.SystemAdmin.id.toShort)
            .map(_.id)
            .result
          // Delete preferences.
          _ <- userPreferenceManager.deletePreferencesForUsers(userIds)
          // Delete user entries.
          _ <- PersistenceSchema.users.filter(_.id inSet userIds).delete
        } yield ()
      }.transactionally
    }
  }

  /**
   * Check to see that migrated users exist.
   */
  def migratedAdministratorsExist(): Future[Boolean] = {
    DB.run(PersistenceSchema.users
      .filter(_.role === UserRole.SystemAdmin.id.toShort)
      .filter(_.ssoStatus =!= Enums.UserSSOStatus.NotMigrated.id.toShort)
      .exists
      .result)
  }

}