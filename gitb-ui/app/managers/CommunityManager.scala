package managers

import models.Enums._
import models._
import org.slf4j.LoggerFactory
import persistence.db._

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

object CommunityManager extends BaseManager {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("CommunityManager")

  /**
   * Gets all communities with given ids or all if none specified
   */
  def getCommunities(ids: Option[List[Long]]): List[Communities] = {
    val q = ids match {
      case Some(idList) => {
        PersistenceSchema.communities
          .filter(_.id inSet idList)
      }
      case None => {
        PersistenceSchema.communities
      }
    }
    exec(q.sortBy(_.shortname.asc)
      .result.map(_.toList))
  }

  def getById(id: Long): Option[Communities] = {
    exec(PersistenceSchema.communities.filter(_.id === id).result.headOption)
  }

  /**
   * Gets the user community
   */
  def getUserCommunity(userId: Long): Community = {
    val u = exec(PersistenceSchema.users.filter(_.id === userId).result.head)
    val o = exec(PersistenceSchema.organizations.filter(_.id === u.organization).result.head)
    val c = exec(PersistenceSchema.communities.filter(_.id === o.community).result.head)
    val d = exec(PersistenceSchema.domains.filter(_.id === c.domain).result.headOption)
    val community = new Community(c, d)
    community
  }

  /**
   *  Creates new community
   */
  def createCommunity(community: Communities) = {
    exec((for {
      communityId <- PersistenceSchema.insertCommunity += community
      _ <- {
        val adminOrganization = Organizations(0L, Constants.AdminOrganizationName, Constants.AdminOrganizationName, OrganizationType.Vendor.id.toShort, true, None, None, None, communityId)
        PersistenceSchema.insertOrganization += adminOrganization
      }
    } yield()).transactionally)
  }

  /**
    * Gets community with specified id
    */
  def getCommunityById(communityId: Long): Community = {
    val c = exec(PersistenceSchema.communities.filter(_.id === communityId).result.head)
    val d = exec(PersistenceSchema.domains.filter(_.id === c.domain).result.headOption)
    val community = new Community(c, d)
    community
  }

  /**
   * Update community
   */
  def updateCommunity(communityId: Long, shortName: String, fullName: String, supportEmail: Option[String], domainId: Option[Long]) = {
    val actions = new ListBuffer[DBIO[_]]()

    val community = exec(PersistenceSchema.communities.filter(_.id === communityId).result.headOption)

    if (community.isDefined) {
      if (!shortName.isEmpty && community.get.shortname != shortName) {
        val q = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.shortname)
        actions += q.update(shortName)
        actions += TestResultManager.updateForUpdatedCommunity(communityId, shortName)
      }

      if (!fullName.isEmpty && community.get.fullname != fullName) {
        val q = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.fullname)
        actions += q.update(fullName)
      }
      val qs = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.supportEmail)
      actions += qs.update(supportEmail)

      val q = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.domain)
      actions += q.update(domainId)

      exec(DBIO.seq(actions.map(a => a): _*).transactionally)
    } else {
      throw new IllegalArgumentException("Community with ID '" + communityId + "' not found")
    }
  }

  /**
   * Deletes the community with specified id
   */
  def deleteCommunity(communityId: Long) {
    exec(
      (
        OrganizationManager.deleteOrganizationByCommunity(communityId) andThen
        LandingPageManager.deleteLandingPageByCommunity(communityId) andThen
        LegalNoticeManager.deleteLegalNoticeByCommunity(communityId) andThen
        ErrorTemplateManager.deleteErrorTemplateByCommunity(communityId) andThen
        TestResultManager.updateForDeletedCommunity(communityId) andThen
        ConformanceManager.deleteConformanceCertificateSettings(communityId) andThen
        PersistenceSchema.communities.filter(_.id === communityId).delete
      ).transactionally
    )
  }

}