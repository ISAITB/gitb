package managers

import models.Enums.UserRole
import models.Enums.UserRole._
import persistence.AccountManager

import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import models._
import persistence.db.PersistenceSchema
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
 * Created by VWYNGAET on 25/10/2016.
 */
object UserManager extends BaseManager {
  def logger = LoggerFactory.getLogger("UserManager")

  def getSystemAdministrators(): Future[List[Users]] = getUsersByRole(UserRole.SystemAdmin)

  /**
   * Gets all users with specified role
   */
  def getUsersByRole(role: UserRole): Future[List[Users]] = {
    Future {
      DB.withSession { implicit session =>
        val users = PersistenceSchema.users.filter(_.role === role.id.toShort).list
        users
      }
    }
  }

  /**
   * Gets all users of specified organization
   */
  def getUsersByOrganization(orgId: Long): Future[List[Users]] = {
    Future {
      DB.withSession { implicit session =>
        val users = PersistenceSchema.users.filter(_.organization === orgId).list
        users
      }
    }
  }

  /**
   * Gets user with specified id
   */
  def getUserById(userId: Long): Future[User] = {
    Future {
      DB.withSession { implicit session =>
        val u = PersistenceSchema.users.filter(_.id === userId).firstOption.get
        val o = PersistenceSchema.organizations.filter(_.id === u.organization).firstOption.get
        val user = new User(u, o)
        user
      }
    }
  }

  /**
   * Updates system admin profile of given user
   */
  def updateSystemAdminProfile(userId: Long, name: String): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        AccountManager.checkUserRole(userId, UserRole.SystemAdmin) map { userExists =>
          if (userExists) {
            val user = PersistenceSchema.users.filter(_.id === userId).firstOption.get

            if (!name.isEmpty && name != user.name) {
              val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
              q.update(name)
            }
          }
        }
      }
    }
  }

  /**
   * Updates user profile of given user
   */
  def updateUserProfile(userId: Long, name: String, roleId: Short): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        val user = PersistenceSchema.users.filter(_.id === userId).firstOption

        if (user.isDefined) {
          if (!name.isEmpty && name != user.get.name) {
            val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
            q.update(name)
          }

          if (UserRole(roleId) != UserRole(user.get.role)) {
            val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.role)
            q.update(roleId)
          }
        }
      }
    }
  }

  /*
   * Create system admin
   */
  def createSystemAdmin(user: Users): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        PersistenceSchema.insertUser += user
      }
    }
  }

  /**
   * Creates new user
   */
  def createUser(user: Users, orgId: Long): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        OrganizationManager.checkOrganizationExists(orgId) map { organizationExists =>
          if (organizationExists) {
            PersistenceSchema.insertUser += user.withOrganizationId(orgId)
          } else {
            throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
          }
        }
      }
    }
  }

  /**
   * Deletes user
   */
  def deleteUser(userId: Long) = Future[Unit] {
    Future {
      DB.withSession { implicit session =>
        PersistenceSchema.users.filter(_.id === userId).delete
      }
    }
  }

  /**
   * Deletes all users with specified organization
   */
  def deleteUserByOrganization(orgId: Long) = Future[Unit] {
    Future {
      DB.withSession { implicit session =>
        PersistenceSchema.users.filter(_.organization === orgId).delete
      }
    }
  }

  /**
   * Checks if user exists
   */
  def checkUserExists(userId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.users.filter(_.id === userId).firstOption
        firstOption.isDefined
      }
    }
  }

  /**
   * Checks if last vendor admin for organization
   *
   */
  def isLastAdmin(userId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        var result = false
        val user = PersistenceSchema.users.filter(_.id === userId).filter(_.role === UserRole.VendorAdmin.id.toShort).firstOption

        if (user.isDefined) {
          val orgId = PersistenceSchema.users.filter(_.id === userId).firstOption.get.organization
          val size = PersistenceSchema.users.filter(_.organization === orgId).filter(_.role === UserRole.VendorAdmin.id.toShort).filter(_.id =!= userId).size.run
          result = size == 0
        }
        result
      }
    }
  }

}