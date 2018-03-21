package managers

import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by VWYNGAET on 25/11/2016.
 */
object LandingPageManager extends BaseManager {
  def logger = LoggerFactory.getLogger("landingPageManager")

  /**
   * Gets all landing pages for the specified community
   */
  def getLandingPagesByCommunity(communityId: Long): List[LandingPages] = {
    DB.withSession { implicit session =>
      val pages = PersistenceSchema.landingPages.filter(_.community === communityId).list
      pages
    }
  }

  /**
   * Checks if a landing page with given name exists for the given community
   */
  def checkUniqueName(name: String, communityId: Long): Boolean = {
    DB.withSession { implicit session =>
      val firstOption = PersistenceSchema.landingPages.filter(_.community === communityId).filter(_.name === name).firstOption
      firstOption.isEmpty
    }
  }

  /**
    * Creates new landing page
    */
  def createLandingPage(landingPage: LandingPages) = {
    DB.withTransaction { implicit session =>
      if (landingPage.default) {
        val q = for {l <- PersistenceSchema.landingPages if l.default === true && l.community === landingPage.community} yield (l.default)
        q.update(false)
      }
      PersistenceSchema.insertLandingPage += landingPage
    }
  }

  /**
    * Gets landing page with specified id
    */
  def getLandingPageById(pageId: Long): LandingPage = {
    DB.withSession { implicit session =>
      val p = PersistenceSchema.landingPages.filter(_.id === pageId).firstOption.get
      val page = new LandingPage(p)
      page
    }
  }

  /**
   * Checks if a landing page with given name exists for the given community
   */
  def checkUniqueName(pageId: Long, name: String, communityId: Long): Boolean = {
    DB.withSession { implicit session =>
      val firstOption = PersistenceSchema.landingPages.filter(_.community === communityId).filter(_.id =!= pageId).filter(_.name === name).firstOption
      firstOption.isEmpty
    }
  }

  /**
   * Updates landing page
   */
  def updateLandingPage(pageId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    DB.withTransaction { implicit session =>
      val landingPageOption = PersistenceSchema.landingPages.filter(_.id === pageId).firstOption
      if (landingPageOption.isDefined) {
        val landingPage = landingPageOption.get

        if (!name.isEmpty && landingPage.name != name) {
          val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.name)
          q.update(name)
        }

        if (content != landingPage.content) {
          val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.content)
          q.update(content)
        }

        if (!landingPage.default && default) {
          var q = for {l <- PersistenceSchema.landingPages if l.default === true && l.community === communityId} yield (l.default)
          q.update(false)

          q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.default)
          q.update(default)
        }

        val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.description)
        q.update(description)
      }
    }
  }

  /**
   * Deletes landing page with specified id
   */
  def deleteLandingPage(pageId: Long) {
    DB.withTransaction { implicit session =>
      PersistenceSchema.landingPages.filter(_.id === pageId).delete
    }
  }

  /**
    * Gets the default landing page for given community
    */
  def getCommunityDefaultLandingPage(communityId: Long): LandingPage = {
    DB.withSession { implicit session =>
      val p = PersistenceSchema.landingPages.filter(_.community === communityId).filter(_.default === true).firstOption
      val defaultPage = p match {
        case Some(p) => new LandingPage(p)
        case None => null
      }
      defaultPage
    }
  }

  def deleteLandingPageByCommunity(communityId: Long)(implicit session: Session) = {
    PersistenceSchema.landingPages.filter(_.community === communityId).delete
  }

}