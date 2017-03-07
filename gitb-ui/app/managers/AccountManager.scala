package persistence

import managers.BaseManager
import models.Enums.UserRole.UserRole
import org.mindrot.jbcrypt.BCrypt
import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import models._
import models.Enums._
import exceptions._
import scala.concurrent.Future
import persistence.db.PersistenceSchema
import org.slf4j.LoggerFactory

object AccountManager extends BaseManager {
  def logger = LoggerFactory.getLogger("AccountManager")

  def registerVendor(organization: Organizations, admin: Users): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        //1) Persist Organization
        val orgId = PersistenceSchema.insertOrganization += organization

        //2) Persist Admin
        PersistenceSchema.insertUser += admin.withOrganizationId(orgId)
      }
    }
  }

  def getVendorProfile(userId: Long): Future[Organization] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get organization id
        val orgId = PersistenceSchema.users.filter(_.id === userId).firstOption.get.organization

        //2) Get Admin info
        val admin = PersistenceSchema.users.filter(_.organization === orgId).filter(_.role === UserRole.VendorAdmin.id.toShort).firstOption

        //3) Get System info
        val systems: List[Systems] = PersistenceSchema.systems.filter(_.owner === orgId).list

        //4) Get Organization info
        val org = PersistenceSchema.organizations.filter(_.id === orgId).firstOption.get

        //5) Get Landing Page info
        val page = PersistenceSchema.landingPages.filter(_.id === org.landingPage).firstOption

        //6) Get Legal Notice info
        val ln = PersistenceSchema.legalNotices.filter(_.id === org.legalNotice).firstOption

        new Organization(org, systems, admin.getOrElse(null), page.getOrElse(null), ln.getOrElse(null))
      }
    }
  }

  def updateVendorProfile(adminId: Long, vendorSname: Option[String], vendorFname: Option[String]): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get organization id of the admin
        val orgId = PersistenceSchema.users.filter(_.id === adminId).firstOption.get.organization

        //2) Update organization table
        if (vendorSname.isDefined) {
          val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.shortname)
          q.update(vendorSname.get)
        }

        if (vendorFname.isDefined) {
          val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.fullname)
          q.update(vendorFname.get)
        }
      }
    }
  }

  def registerUser(adminId: Long, user: Users): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get organization id of the admin
        val orgId = PersistenceSchema.users.filter(_.id === adminId).firstOption.get.organization

        //2) Insert new user to Users table
        PersistenceSchema.insertUser += user.withOrganizationId(orgId)
      }
    }
  }

  def getUserProfile(userId: Long): Future[User] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get User info
        val u = PersistenceSchema.users.filter(_.id === userId).firstOption.get

        //2) Get Organization info
        val o = PersistenceSchema.organizations.filter(_.id === u.organization).firstOption.get

        //3) Merge User and Organization info
        val user = new User(u, o)
        user
      }
    }
  }

  def updateUserProfile(userId: Long, name: Option[String], password: Option[String], oldpassword: Option[String]): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        //1) Update name of the user
        if (name.isDefined) {
          val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.name)
          q.update(name.get)
        }
        //2) Update password of the user
        if (password.isDefined && oldpassword.isDefined) {
          //2.1) but first, check his old password if it is correct
          val user = PersistenceSchema.users.filter(_.id === userId).firstOption
          if (user.isDefined && BCrypt.checkpw(oldpassword.get, user.get.password)) {
            //2.1.1) password correct, replace it with the new one
            val q = for {u <- PersistenceSchema.users if u.id === userId} yield (u.password)
          } else {
            //2.1.2) incorrect password => send Invalid Credentials error
            throw InvalidAuthorizationException(ErrorCodes.INVALID_CREDENTIALS, "Invalid credentials")
          }
        }
      }
    }
  }

  def getVendorUsers(userId: Long): Future[List[Users]] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get organization id of the user first
        val orgId = PersistenceSchema.users.filter(_.id === userId).firstOption.get.organization

        //2) Get all users of the organization
        val users = PersistenceSchema.users.filter(_.organization === orgId).list
        users
      }
    }
  }

  def isVendorAdmin(userId: Long) = checkUserRole(userId, UserRole.VendorAdmin)

  def isSystemAdmin(userId: Long) = checkUserRole(userId, UserRole.SystemAdmin)

  def checkUserRole(userId: Long, role: UserRole): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val option = PersistenceSchema.users.filter(_.id === userId).firstOption

        option.isDefined && option.get.role == role.id.toShort
      }
    }
  }
}
