package managers

import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import models._
import persistence.db.PersistenceSchema
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
 * Created by VWYNGAET on 26/10/2016.
 */
object OrganizationManager extends BaseManager {
  def logger = LoggerFactory.getLogger("OrganizationManager")

  /**
   * Checks if organization exists (ignoring the default)
   */
  def checkOrganizationExists(orgId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.organizations.filter(_.id =!= Constants.DefaultOrganizationId).filter(_.id === orgId).firstOption
        firstOption.isDefined
      }
    }
  }

  /**
   * Gets all organizations
   */
  def getOrganizations(): Future[List[Organizations]] = {
    Future {
      DB.withSession { implicit session =>
        //1) Get all organizations except the default organization for system administrators
        val organizations = PersistenceSchema.organizations.filter(_.id =!= Constants.DefaultOrganizationId).list
        organizations
      }
    }
  }

  /**
   * Gets user with specified id
   */
  def getOrganizationById(orgId: Long): Future[Organization] = {
    Future {
      DB.withSession { implicit session =>
        val o = PersistenceSchema.organizations.filter(_.id === orgId).firstOption.get
        val organization = new Organization(o)
        organization
      }
    }
  }

  /**
   * Creates new organization
   */
  def createOrganization(organization:Organizations): Future[Unit] = {
    Future{
      DB.withSession { implicit session =>
        PersistenceSchema.insertOrganization += organization
      }
    }
  }

  def updateOrganization(orgId: Long, shortName: String, fullName: String): Future[Unit] = {
    Future{
      DB.withSession { implicit session =>
        val org = PersistenceSchema.organizations.filter(_.id === orgId).firstOption

        if (org.isDefined) {
          if (!shortName.isEmpty && org.get.shortname != shortName) {
            val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.shortname)
            q.update(shortName)
          }

          if (!fullName.isEmpty && org.get.fullname != fullName) {
            val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.fullname)
            q.update(fullName)
          }
        } else {
          throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
        }

      }
    }
  }

  /**
   * Deletes organization with specified id
   */
  def deleteOrganization(orgId: Long) = Future[Unit] {
    Future {
      DB.withSession { implicit session =>
        checkOrganizationExists(orgId) map { organizationExists =>
          if(organizationExists) {
            PersistenceSchema.organizations.filter(_.id === orgId).delete
            UserManager.deleteUserByOrganization(orgId)
            SystemManager.deleteSystemByOrganization(orgId)
          } else {
            throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
          }
        }
      }
    }
  }

}