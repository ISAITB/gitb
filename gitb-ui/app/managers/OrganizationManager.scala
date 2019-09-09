package managers

import javax.inject.{Inject, Singleton}
import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by VWYNGAET on 26/10/2016.
 */
@Singleton
class OrganizationManager @Inject() (systemManager: SystemManager, testResultManager: TestResultManager, dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("OrganizationManager")

  /**
   * Checks if organization exists (ignoring the default)
   */
  def checkOrganizationExists(orgId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.organizations.filter(_.id =!= Constants.DefaultOrganizationId).filter(_.id === orgId).result.headOption)
    firstOption.isDefined
  }

  /**
   * Gets all organizations
   */
  def getOrganizations(): List[Organizations] = {
    //1) Get all organizations except the default organization for system administrators
    val organizations = exec(PersistenceSchema.organizations
        .sortBy(_.shortname.asc)
        .result.map(_.toList))
    organizations
  }

  def getOrganisationTemplates(communityId: Long): Option[List[SelfRegTemplate]] = {
    val result = exec(PersistenceSchema.organizations
      .filter(_.community === communityId)
      .filter(_.template === true)
      .sortBy(_.templateName).result
    ).map(x => new SelfRegTemplate(x.id, x.templateName.get)).toList
    if (result.isEmpty) {
      None
    } else {
      Some(result)
    }
  }

  /**
   * Gets organizations with specified community
   */
  def getOrganizationsByCommunity(communityId: Long): List[Organizations] = {
    val organizations = exec(PersistenceSchema.organizations.filter(_.adminOrganization === false).filter(_.community === communityId)
      .sortBy(_.shortname.asc)
      .result.map(_.toList))
    organizations
  }

  def getById(id: Long): Option[Organizations] = {
    exec(PersistenceSchema.organizations.filter(_.id === id).result.headOption)
  }

  /**
   * Gets organization with specified id
   */
  def getOrganizationById(orgId: Long): Organization = {
    val o = exec(PersistenceSchema.organizations.filter(_.id === orgId).result.head)
    val l = exec(PersistenceSchema.landingPages.filter(_.id === o.landingPage).result.headOption)
    val n = exec(PersistenceSchema.legalNotices.filter(_.id === o.legalNotice).result.headOption)
    val e = exec(PersistenceSchema.errorTemplates.filter(_.id === o.errorTemplate).result.headOption)
    val organization = new Organization(o, l.orNull, n.orNull, e.orNull)
    organization
  }

  private def copyTestSetup(fromOrganisation: Long, toOrganisation: Long) = {
    val actions = new ListBuffer[DBIO[_]]()
    val systems = systemManager.getSystemsByOrganization(fromOrganisation)
    systems.foreach { otherSystem =>
      actions += (
        for {
          newSystemId <- systemManager.registerSystem(Systems(0L, otherSystem.shortname, otherSystem.fullname, otherSystem.description, otherSystem.version, toOrganisation))
          _ <- systemManager.copyTestSetup(otherSystem.id, newSystemId)
        } yield()
      )
    }
    DBIO.seq(actions.map(a => a): _*)
  }

  def createOrganizationInTrans(organization: Organizations, otherOrganisationId: Option[Long]) = {
    for {
      newOrganisationId <- PersistenceSchema.insertOrganization += organization
      _ <- {
        if (otherOrganisationId.isDefined) {
          copyTestSetup(otherOrganisationId.get, newOrganisationId)
        } else {
          DBIO.successful(())
        }
      }
    } yield newOrganisationId
  }

  /**
   * Creates new organization
   */
  def createOrganization(organization: Organizations, otherOrganisationId: Option[Long]) = {
    val id: Long = exec(
      createOrganizationInTrans(organization, otherOrganisationId).transactionally
    )
    id
  }

  def isTemplateNameUnique(templateName: String, communityId: Long, organisationIdToIgnore: Option[Long]): Boolean = {
    var q = PersistenceSchema.organizations
      .filter(_.community === communityId)
      .filter(_.template === true)
      .filter(_.templateName === templateName)
    if (organisationIdToIgnore.isDefined) {
      q = q.filter(_.id =!= organisationIdToIgnore.get)
    }
    val result = exec(q.result.headOption)
    result.isEmpty
  }

  def updateOrganization(orgId: Long, shortName: String, fullName: String, landingPageId: Option[Long], legalNoticeId: Option[Long], errorTemplateId: Option[Long], otherOrganisation: Option[Long], template: Boolean, templateName: Option[String]) = {
    val org = exec(PersistenceSchema.organizations.filter(_.id === orgId).result.headOption)
    if (org.isDefined) {
      val actions = new ListBuffer[DBIO[_]]()
      if (!shortName.isEmpty && org.get.shortname != shortName) {
        val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.shortname)
        actions += q.update(shortName)
        actions += testResultManager.updateForUpdatedOrganisation(orgId, shortName)
      }
      if (!fullName.isEmpty && org.get.fullname != fullName) {
        val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.fullname)
        actions += q.update(fullName)
      }
      var templateNameToSet: Option[String] = null
      if (template) {
        templateNameToSet = templateName
      } else {
        templateNameToSet = None
      }
      val q = for {o <- PersistenceSchema.organizations if o.id === orgId} yield (o.landingPage, o.legalNotice, o.errorTemplate, o.template, o.templateName)
      actions += q.update(landingPageId, legalNoticeId, errorTemplateId, template, templateNameToSet)
      if (otherOrganisation.isDefined) {
        // Replace the test setup for the organisation with the one from the provided one.
        actions += systemManager.deleteSystemByOrganization(orgId)
        actions += copyTestSetup(otherOrganisation.get, orgId)
      }
      exec(DBIO.seq(actions.map(a => a): _*).transactionally)
    } else {
      throw new IllegalArgumentException("Organization with ID '" + orgId + "' not found")
    }
  }

  /**
   * Deletes organization by community
   */
  def deleteOrganizationByCommunity(communityId: Long) = {
    testResultManager.updateForDeletedOrganisationByCommunityId(communityId)
    for {
      list <- PersistenceSchema.organizations.filter(_.community === communityId).result
      _ <- DBIO.seq(list.map { org =>
        deleteUserByOrganization(org.id) andThen
        systemManager.deleteSystemByOrganization(org.id) andThen
        PersistenceSchema.organizations.filter(_.community === communityId).delete
      }: _*)
    } yield()
  }

  /**
   * Deletes organization with specified id
   */
  def deleteOrganization(orgId: Long) {
    exec(
      (
        testResultManager.updateForDeletedOrganisation(orgId) andThen
        deleteUserByOrganization(orgId) andThen
        systemManager.deleteSystemByOrganization(orgId) andThen
        PersistenceSchema.organizations.filter(_.id === orgId).delete
      ).transactionally
    )
  }

  /**
    * Deletes all users with specified organization
    */
  def deleteUserByOrganization(orgId: Long) = {
    PersistenceSchema.users.filter(_.organization === orgId).delete
  }

}