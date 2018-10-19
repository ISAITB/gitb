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
      val organizations = PersistenceSchema.organizations
          .sortBy(_.shortname.asc)
        .list
      organizations
    }
  }

  /**
   * Gets organizations with specified community
   */
  def getOrganizationsByCommunity(communityId: Long): List[Organizations] = {
    DB.withSession { implicit session =>
      val organizations = PersistenceSchema.organizations.filter(_.adminOrganization === false).filter(_.community === communityId)
          .sortBy(_.shortname.asc)
        .list
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
      val e = PersistenceSchema.errorTemplates.filter(_.id === o.errorTemplate).firstOption
      val organization = new Organization(o, l.getOrElse(null), n.getOrElse(null), e.getOrElse(null))
      organization
    }
  }

  private def copyTestSetup(fromOrganisation: Long, toOrganisation: Long)(implicit session:Session) = {
    val systems = SystemManager.getSystemsByOrganization(fromOrganisation)
    systems.foreach { otherSystem =>
      val newSystemId = SystemManager.registerSystem(Systems(0L, otherSystem.shortname, otherSystem.fullname, otherSystem.description, otherSystem.version, toOrganisation))
      SystemManager.copyTestSetup(otherSystem.id, newSystemId)
    }
  }

  /**
   * Creates new organization
   */
  def createOrganization(organization: Organizations, otherOrganisationId: Option[Long]) = {
    DB.withTransaction { implicit session =>
      val newOrganisationId = PersistenceSchema.insertOrganization += organization
      if (otherOrganisationId.isDefined) {
        copyTestSetup(otherOrganisationId.get, newOrganisationId)
      }
      newOrganisationId
    }
  }

  def updateOrganization(orgId: Long, shortName: String, fullName: String, landingPageId: Option[Long], legalNoticeId: Option[Long], errorTemplateId: Option[Long], otherOrganisation: Option[Long]) = {
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
        val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.landingPage, o.legalNotice, o.errorTemplate)
        q.update(landingPageId, legalNoticeId, errorTemplateId)
        if (otherOrganisation.isDefined) {
          // Replace the test setup for the organisation with the one from the provided one.
          SystemManager.deleteSystemByOrganization(orgId)
          copyTestSetup(otherOrganisation.get, orgId)
        }
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