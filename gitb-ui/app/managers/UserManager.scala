package managers

import models.Enums.UserRole
import models.Enums.UserRole._
import models._
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import persistence.AccountManager
import persistence.db.PersistenceSchema

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by VWYNGAET on 25/10/2016.
 */
object UserManager extends BaseManager {

  import dbConfig.driver.api._

  def logger = LoggerFactory.getLogger("UserManager")

  def getSystemAdministrators(): List[Users] = getUsersByRole(UserRole.SystemAdmin)

  /**
    * Gets all community administrators of the given community
    */
  def getCommunityAdministrators(communityId:Long): List[Users] = {
    val organizations = exec(PersistenceSchema.organizations.filter(_.community === communityId).map(_.id).result.map(_.toList))
    val users = exec(PersistenceSchema.users.filter(_.organization inSet organizations).filter(_.role === UserRole.CommunityAdmin.id.toShort)
        .sortBy(_.name.asc)
        .result
        .map(_.toList))
    users
  }

  /**
   * Gets all users with specified role
   */
  def getUsersByRole(role: UserRole): List[Users] = {
    val users = exec(PersistenceSchema.users.filter(_.role === role.id.toShort)
      .sortBy(_.name.asc)
      .result.map(_.toList))
    users
  }

  /**
   * Gets all users of specified organization
   */
  def getUsersByOrganization(orgId: Long): List[Users] = {
    val users = exec(PersistenceSchema.users.filter(_.organization === orgId).filter(x => x.role === UserRole.VendorUser.id.toShort || x.role === UserRole.VendorAdmin.id.toShort)
      .sortBy(_.name.asc)
      .result.map(_.toList))
    users
  }

  /**
   * Gets user with specified id
   */
  def getUserById(userId: Long): User = {
    val u = exec(PersistenceSchema.users.filter(_.id === userId).result.head)
    val o = exec(PersistenceSchema.organizations.filter(_.id === u.organization).result.head)
    val user = new User(u, o)
    user
  }

  /**
   * Updates system admin profile of given user
   */
  def updateSystemAdminProfile(userId: Long, name: String, password: Option[String]) = {
    val userExists = AccountManager.checkUserRole(userId, UserRole.SystemAdmin)
    if (userExists) {
      var action: DBIO[_] = null
      if (password.isDefined) {
        val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name, u.password, u.onetimePassword)
        action = q.update(name, BCrypt.hashpw(password.get, BCrypt.gensalt()), true)
      } else {
        val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
        action = q.update(name)
      }
      exec(action.transactionally)
    }
  }

  /**
   * Updates community admin profile of given user
   */
  def updateCommunityAdminProfile(userId: Long, name: String, password: Option[String]) = {
    val userExists = AccountManager.checkUserRole(userId, UserRole.CommunityAdmin)
    if (userExists) {
      var action: DBIO[_] = null
      if (password.isDefined) {
        val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name, u.password, u.onetimePassword)
        action = q.update(name, BCrypt.hashpw(password.get, BCrypt.gensalt()), true)
      } else {
        val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
        action = q.update(name)
      }
      exec(action.transactionally)
    }
  }

  /**
   * Updates user profile of given user
   */
  def updateUserProfile(userId: Long, name: String, roleId: Short, password: Option[String]) = {
    val user = exec(PersistenceSchema.users.filter(_.id === userId).result.headOption)
    if (user.isDefined) {
      var action: DBIO[_] = null
      if (password.isDefined) {
        val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name, u.role, u.password, u.onetimePassword)
        action = q.update(name, roleId, BCrypt.hashpw(password.get, BCrypt.gensalt()), true)
      } else {
        val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name, u.role)
        action = q.update(name, roleId)
      }
      exec(action.transactionally)
    }
  }

  /**
   * Create system admin
   */
  def createAdmin(user: Users, communityId: Long) = {
    val o = exec(PersistenceSchema.organizations.filter(_.community === communityId).filter(_.adminOrganization === true).result.head)
    exec((PersistenceSchema.insertUser += user.withOrganizationId(o.id)).transactionally)
  }

  /**
   * Creates new user
   */
  def createUser(user: Users, orgId: Long) = {
    val organizationExists = OrganizationManager.checkOrganizationExists(orgId)
    if (organizationExists) {
      exec((PersistenceSchema.insertUser += user.withOrganizationId(orgId)).transactionally)
    } else {
      throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
    }
  }

  /**
   * Deletes user
   */
  def deleteUser(userId: Long) = {
    exec(PersistenceSchema.users.filter(_.id === userId).delete.transactionally)
  }

  /**
   * Deletes all users with specified organization
   */
  def deleteUserByOrganization(orgId: Long) = {
    PersistenceSchema.users.filter(_.organization === orgId).delete
  }

  /**
   * Checks if user exists
   */
  def checkUserExists(userId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.users.filter(_.id === userId).result.headOption)
    firstOption.isDefined
  }

  /**
   * Checks if last vendor admin for organization
   *
   */
  def isLastAdmin(userId: Long): Boolean = {
    var result = false
    val user = exec(PersistenceSchema.users.filter(_.id === userId).filter(_.role === UserRole.VendorAdmin.id.toShort).result.headOption)

    if (user.isDefined) {
      val orgId = exec(PersistenceSchema.users.filter(_.id === userId).result.head).organization
      val size = exec(PersistenceSchema.users.filter(_.organization === orgId).filter(_.role === UserRole.VendorAdmin.id.toShort).filter(_.id =!= userId).size.result)
      result = size == 0
    }
    result
  }

}