package managers

import models.Enums.UserRole
import models.Enums.UserRole._

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

  def getUsersByRole(role: UserRole): Future[List[Users]] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get all users by role
        val users = PersistenceSchema.users.filter(_.role === role.id.toShort).list
        users
      }
    }
  }

  def getUsersByOrganization(orgId: Long): Future[List[Users]] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get all users of the organization
        val users = PersistenceSchema.users.filter(_.organization === orgId).list
        users
      }
    }
  }

}