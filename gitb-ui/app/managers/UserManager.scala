package managers

import models.Enums.UserRole
import models.Enums.UserRole._
import models._
import org.slf4j.LoggerFactory
import persistence.AccountManager
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by VWYNGAET on 25/10/2016.
 */
object UserManager extends BaseManager {
  def logger = LoggerFactory.getLogger("UserManager")

  def getSystemAdministrators(): List[Users] = getUsersByRole(UserRole.SystemAdmin)

  /**
    * Gets all community administrators of the given community
    */
  def getCommunityAdministrators(communityId:Long): List[Users] = {
    DB.withSession { implicit session =>
      val organizations = PersistenceSchema.organizations.filter(_.community === communityId).map(_.id).list
      val users = PersistenceSchema.users.filter(_.organization inSet organizations).filter(_.role === UserRole.CommunityAdmin.id.toShort).list
      users
    }
  }

  /**
   * Gets all users with specified role
   */
  def getUsersByRole(role: UserRole): List[Users] = {
    DB.withSession { implicit session =>
      val users = PersistenceSchema.users.filter(_.role === role.id.toShort).list
      users
    }
  }

  /**
   * Gets all users of specified organization
   */
  def getUsersByOrganization(orgId: Long): List[Users] = {
    DB.withSession { implicit session =>
      val users = PersistenceSchema.users.filter(_.organization === orgId).filter(x => x.role === UserRole.VendorUser.id.toShort || x.role === UserRole.VendorAdmin.id.toShort).list
      users
    }
  }

  /**
   * Gets user with specified id
   */
  def getUserById(userId: Long): User = {
    DB.withSession { implicit session =>
      val u = PersistenceSchema.users.filter(_.id === userId).firstOption.get
      val o = PersistenceSchema.organizations.filter(_.id === u.organization).firstOption.get
      val user = new User(u, o)
      user
    }
  }

  /**
   * Updates system admin profile of given user
   */
  def updateSystemAdminProfile(userId: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val userExists = AccountManager.checkUserRole(userId, UserRole.SystemAdmin)
      if (userExists) {
        val user = PersistenceSchema.users.filter(_.id === userId).firstOption.get

        if (!name.isEmpty && name != user.name) {
          val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
          q.update(name)
        }
      }
    }
  }

  /**
   * Updates community admin profile of given user
   */
  def updateCommunityAdminProfile(userId: Long, name: String) = {
    DB.withTransaction { implicit session =>
      val userExists = AccountManager.checkUserRole(userId, UserRole.CommunityAdmin)
      if (userExists) {
        val user = PersistenceSchema.users.filter(_.id === userId).firstOption.get

        if (!name.isEmpty && name != user.name) {
          val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
          q.update(name)
        }
      }
    }
  }

  /**
   * Updates user profile of given user
   */
  def updateUserProfile(userId: Long, name: String, roleId: Short) = {
    DB.withTransaction { implicit session =>
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

  /**
   * Create system admin
   */
  def createAdmin(user: Users, communityId: Long) = {
    DB.withTransaction { implicit session =>
      val o = PersistenceSchema.organizations.filter(_.community === communityId).filter(_.adminOrganization === true).firstOption.get
      PersistenceSchema.insertUser += user.withOrganizationId(o.id)
    }
  }

  /**
   * Creates new user
   */
  def createUser(user: Users, orgId: Long) = {
    DB.withTransaction { implicit session =>
      val organizationExists = OrganizationManager.checkOrganizationExists(orgId)
      if (organizationExists) {
        PersistenceSchema.insertUser += user.withOrganizationId(orgId)
      } else {
        throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
      }
    }
  }

  /**
   * Deletes user
   */
  def deleteUser(userId: Long) = {
    DB.withTransaction { implicit session =>
      PersistenceSchema.users.filter(_.id === userId).delete
    }
  }

  /**
   * Deletes all users with specified organization
   */
  def deleteUserByOrganization(orgId: Long)(implicit session: Session) = {
    PersistenceSchema.users.filter(_.organization === orgId).delete
  }

  /**
   * Checks if user exists
   */
  def checkUserExists(userId: Long): Boolean = {
    DB.withSession { implicit session =>
      val firstOption = PersistenceSchema.users.filter(_.id === userId).firstOption
      firstOption.isDefined
    }
  }

  /**
   * Checks if last vendor admin for organization
   *
   */
  def isLastAdmin(userId: Long): Boolean = {
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