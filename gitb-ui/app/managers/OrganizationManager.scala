package managers

import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by VWYNGAET on 26/10/2016.
 */
object OrganizationManager extends BaseManager {
  def logger = LoggerFactory.getLogger("OrganizationManager")

  /**
   * Checks if organization exists (ignoring the default)
   */
  def checkOrganizationExists(orgId: Long)(implicit session: Session): Boolean = {
    val firstOption = PersistenceSchema.organizations.filter(_.id =!= Constants.DefaultOrganizationId).filter(_.id === orgId).firstOption
    firstOption.isDefined
  }

  /**
   * Gets all organizations
   */
  def getOrganizations(): List[Organizations] = {
    DB.withSession { implicit session =>
      //1) Get all organizations except the default organization for system administrators
      val organizations = PersistenceSchema.organizations.list
      organizations
    }
  }

  /**
   * Gets organizations with specified community
   */
  def getOrganizationsByCommunity(communityId: Long): List[Organizations] = {
    DB.withSession { implicit session =>
      val organizations = PersistenceSchema.organizations.filter(_.shortname =!= Constants.AdminOrganizationName).filter(_.community === communityId).list
      organizations
    }
  }

  def getById(id: Long): Option[Organizations] = {
    DB.withSession { implicit session =>
      PersistenceSchema.organizations.filter(_.id === id).firstOption
    }
  }

  /**
   * Gets organization with specified id
   */
  def getOrganizationById(orgId: Long): Organization = {
    DB.withSession { implicit session =>
      val o = PersistenceSchema.organizations.filter(_.id === orgId).firstOption.get
      val l = PersistenceSchema.landingPages.filter(_.id === o.landingPage).firstOption
      val n = PersistenceSchema.legalNotices.filter(_.id === o.legalNotice).firstOption
      val organization = new Organization(o, l.getOrElse(null), n.getOrElse(null))
      organization
    }
  }

  /**
   * Creates new organization
   */
  def createOrganization(organization: Organizations) = {
    DB.withTransaction { implicit session =>
      PersistenceSchema.insertOrganization += organization
    }
  }

  def updateOrganization(orgId: Long, shortName: String, fullName: String, landingPageId: Option[Long], legalNoticeId: Option[Long]) = {
    DB.withTransaction { implicit session =>
      val org = PersistenceSchema.organizations.filter(_.id === orgId).firstOption

      if (org.isDefined) {
        if (!shortName.isEmpty && org.get.shortname != shortName) {
          val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.shortname)
          q.update(shortName)

          TestResultManager.updateForUpdatedOrganisation(orgId, shortName)
        }

        if (!fullName.isEmpty && org.get.fullname != fullName) {
          val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.fullname)
          q.update(fullName)
        }

        val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.landingPage, o.legalNotice)
        q.update(landingPageId, legalNoticeId)
      } else {
        throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
      }

    }
  }

  /**
   * Deletes organization by community
   */
  def deleteOrganizationByCommunity(communityId: Long)(implicit session: Session) = {
    TestResultManager.updateForDeletedOrganisationByCommunityId(communityId)
    val list = PersistenceSchema.organizations.filter(_.community === communityId).list
    list foreach { org =>
      UserManager.deleteUserByOrganization(org.id)
      SystemManager.deleteSystemByOrganization(org.id)
      PersistenceSchema.organizations.filter(_.community === communityId).delete
    }
  }

  /**
   * Deletes organization with specified id
   */
  def deleteOrganization(orgId: Long) {
    DB.withTransaction { implicit session =>
      TestResultManager.updateForDeletedOrganisation(orgId)
      UserManager.deleteUserByOrganization(orgId)
      SystemManager.deleteSystemByOrganization(orgId)
      PersistenceSchema.organizations.filter(_.id === orgId).delete
    }
  }

}