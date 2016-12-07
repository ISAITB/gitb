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
   * Gets organization with specified id
   */
  def getOrganizationById(orgId: Long): Future[Organization] = {
    Future {
      DB.withSession { implicit session =>
        val o = PersistenceSchema.organizations.filter(_.id === orgId).firstOption.get
        val l = PersistenceSchema.landingPages.filter(_.id === o.landingPage).firstOption
        val organization = new Organization(o, l.getOrElse(null))
        organization
      }
    }
  }

  /**
   * Creates new organization
   */
  def createOrganization(organization: Organizations): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        PersistenceSchema.insertOrganization += organization
      }
    }
  }

  def updateOrganization(orgId: Long, shortName: String, fullName: String, landingPageId: Option[Long]): Future[Unit] = {
    Future {
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

          val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.landingPage)
          q.update(landingPageId)
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
      DB.withTransaction { implicit session =>
        UserManager.deleteUserByOrganization(orgId)
        SystemManager.deleteSystemByOrganization(orgId)
        PersistenceSchema.organizations.filter(_.id === orgId).delete
      }
    }
  }

}