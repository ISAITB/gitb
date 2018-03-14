package managers

import models.Enums._
import models._
import org.slf4j.LoggerFactory
import persistence.db._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import scala.slick.driver.MySQLDriver.simple._

object CommunityManager extends BaseManager {
  def logger = LoggerFactory.getLogger("CommunityManager")

  /**
   * Gets all communities with given ids or all if none specified
   */
  def getCommunities(ids: Option[List[Long]]): Future[List[Communities]] = {
    Future {
      DB.withSession { implicit session =>
        val q = ids match {
          case Some(idList) => {
            PersistenceSchema.communities
              .filter(_.id inSet idList)
          }
          case None => {
            PersistenceSchema.communities
          }
        }
        q.list
      }
    }
  }

  def getById(id: Long): Option[Communities] = {
    DB.withSession { implicit session =>
      PersistenceSchema.communities.filter(_.id === id).firstOption
    }
  }

  /**
   * Gets the user community
   */
  def getUserCommunity(userId: Long): Future[Community] = {
    Future {
      DB.withSession { implicit session =>
        val u = PersistenceSchema.users.filter(_.id === userId).firstOption.get
        val o = PersistenceSchema.organizations.filter(_.id === u.organization).firstOption.get
        val c = PersistenceSchema.communities.filter(_.id === o.community).firstOption.get
        val d = PersistenceSchema.domains.filter(_.id === c.domain).firstOption
        val community = new Community(c, d)
        community
      }
    }
  }

  /**
   *  Creates new community
   */
  def createCommunity(community: Communities): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        val communityId = PersistenceSchema.insertCommunity += community
        val adminOrganization = Organizations(0L, Constants.AdminOrganizationName, Constants.AdminOrganizationName, OrganizationType.Vendor.id.toShort, true, None, None, communityId)
        PersistenceSchema.insertOrganization += adminOrganization
      }
    }
  }

  /**
    * Gets community with specified id
    */
  def getCommunityById(communityId: Long): Future[Community] = {
    Future {
      DB.withSession { implicit session =>
        val c = PersistenceSchema.communities.filter(_.id === communityId).firstOption.get
        val d = PersistenceSchema.domains.filter(_.id === c.domain).firstOption
        val community = new Community(c, d)
        community
      }
    }
  }

  /**
   * Update community
   */
  def updateCommunity(communityId: Long, shortName: String, fullName: String, domainId: Option[Long]) = {
    DB.withTransaction { implicit session =>
      val community = PersistenceSchema.communities.filter(_.id === communityId).firstOption

      if (community.isDefined) {
        if (!shortName.isEmpty && community.get.shortname != shortName) {
          val q = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.shortname)
          q.update(shortName)

          TestResultManager.updateForUpdatedCommunity(communityId, shortName)
        }

        if (!fullName.isEmpty && community.get.fullname != fullName) {
          val q = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.fullname)
          q.update(fullName)
        }

        val q = for {c <- PersistenceSchema.communities if c.id === communityId} yield (c.domain)
        q.update(domainId)
      } else {
        throw new IllegalArgumentException("Community with ID '" + communityId + "' not found")
      }
    }
  }

  /**
   * Deletes the community with specified id
   */
  def deleteCommunity(communityId: Long) {
    DB.withTransaction { implicit session =>
      OrganizationManager.deleteOrganizationByCommunity(communityId)
      LandingPageManager.deleteLandingPageByCommunity(communityId)
      LegalNoticeManager.deleteLegalNoticeByCommunity(communityId)
      TestResultManager.updateForDeletedCommunity(communityId)
      PersistenceSchema.communities.filter(_.id === communityId).delete
    }
  }

}