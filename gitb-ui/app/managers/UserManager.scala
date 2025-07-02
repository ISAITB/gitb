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

import config.Configurations
import models.Enums.UserRole
import models.Enums.UserRole._
import models._
import org.slf4j.LoggerFactory
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
                             organizationManager: OrganizationManager,
                             dbConfigProvider: DatabaseConfigProvider)
                            (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("UserManager")

  def getSystemAdministrators(): Future[List[Users]] = {
    getUsersByRole(UserRole.SystemAdmin)
  }

  /**
    * Gets all community administrators of the given community
    */
  def getCommunityAdministrators(communityId:Long): Future[List[Users]] = {
    DB.run(for {
      organizations <- PersistenceSchema.organizations.filter(_.community === communityId).map(_.id).result.map(_.toList)
      users <- PersistenceSchema.users.filter(_.organization inSet organizations).filter(_.role === UserRole.CommunityAdmin.id.toShort)
        .sortBy(_.name.asc)
        .result
        .map(_.toList)
    } yield users)
  }

  /**
   * Gets all users with specified role
   */
  def getUsersByRole(role: UserRole): Future[List[Users]] = {
    DB.run(PersistenceSchema.users.filter(_.role === role.id.toShort)
      .sortBy(_.name.asc)
      .result.map(_.toList))
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
      _ <- PersistenceSchema.insertUser += user.withOrganizationId(organisationId)
    } yield ()
    DB.run(dbAction.transactionally)
  }

  /**
   * Creates new user
   */
  def createUser(user: Users, orgId: Long): Future[Long] = {
    val action = for {
      organizationExists <- organizationManager.checkOrganizationExistsInternal(orgId)
      newOrgId <- {
        if (organizationExists) {
          PersistenceSchema.insertUser += user.withOrganizationId(orgId)
        } else {
          throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
        }
      }
    } yield newOrgId
    DB.run(action.transactionally)
  }

  /**
   * Deletes user
   */
  def deleteUser(userId: Long): Future[Int] = {
    DB.run(PersistenceSchema.users.filter(_.id === userId).delete.transactionally)
  }

  def deleteUsersByUidExceptTestBedAdmin(ssoUid: String, ssoEmail: String): Future[Unit] = {
    val dbAction = for {
      // Delete active roles
      _ <- PersistenceSchema.users
        .filter(_.ssoUid === ssoUid)
        .filter(_.role =!= Enums.UserRole.SystemAdmin.id.toShort)
        .delete
      // Delete inactive roles
      _ <- PersistenceSchema.users
        .filter(_.ssoEmail.isDefined)
        .filter(_.ssoEmail.toLowerCase === ssoEmail.toLowerCase)
        .filter(_.role =!= Enums.UserRole.SystemAdmin.id.toShort)
        .delete
    } yield ()
    DB.run(dbAction.transactionally)
  }

  /**
   * Deletes user
   */
  def deleteUserExceptTestBedAdmin(userId: Long): Future[Int] = {
    DB.run(PersistenceSchema.users
      .filter(_.id === userId)
      .filter(_.role =!= Enums.UserRole.SystemAdmin.id.toShort)
      .delete.transactionally)
  }

  /**
   * Check to see that migrated users exist.
   */
  def migratedUsersExist(): Future[Boolean] = {
    DB.run(PersistenceSchema.users
      .filter(_.ssoStatus =!= Enums.UserSSOStatus.NotMigrated.id.toShort)
      .exists
      .result)
  }

}