package managers

import managers.UserManager._
import models.Enums.UserRole
import persistence.AccountManager
import play.api.Play

import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import models._
import persistence.db.PersistenceSchema
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
 * Created by VWYNGAET on 25/11/2016.
 */
object LandingPageManager extends BaseManager {
  def logger = LoggerFactory.getLogger("landingPageManager")

  /**
   * Checks if name exists
   */
  def checkUniqueName(name: String): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.landingPages.filter(_.name === name).firstOption
        !firstOption.isDefined
      }
    }
  }

  /**
   * Checks if page exists
   */
  def checkLandingPageExists(pageId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.landingPages.filter(_.id === pageId).firstOption
        firstOption.isDefined
      }
    }
  }

  /**
   * Checks if page exists
   */
  def checkDefaultLandingPageExists(): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.landingPages.filter(_.default === true).firstOption
        firstOption.isDefined
      }
    }
  }

  /**
   * Gets all landing pages
   */
  def getLandingPages(): Future[List[LandingPages]] = {
    Future {
      DB.withSession { implicit session =>
        val pages = PersistenceSchema.landingPages.list
        pages
      }
    }
  }

  /**
   * Gets landing page with specified id
   */
  def getLandingPageById(pageId: Long): Future[LandingPage] = {
    Future {
      DB.withSession { implicit session =>
        val p = PersistenceSchema.landingPages.filter(_.id === pageId).firstOption.get
        val page = new LandingPage(p)
        page
      }
    }
  }

  /**
   * Gets the default landing page
   */
  def getDefaultLandingPage(): Future[LandingPage] = {
    Future {
      DB.withSession { implicit session =>
        var page:LandingPage = null
        val p = PersistenceSchema.landingPages.filter(_.default === true).firstOption
        if (p.isDefined) {
          page = new LandingPage(p.get)
        }
        page
      }
    }
  }

  /**
   * Creates new landing page
   */
  def createLandingPage(landingPage: LandingPages): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        if (landingPage.default) {
          val q = for {l <- PersistenceSchema.landingPages if l.default === true} yield (l.default)
          q.update(false)
        }

        PersistenceSchema.insertLandingPage += landingPage
      }
    }
  }

  /**
   * Updates landing page
   */
  def updateLandingPage(pageId: Long, name: String, description: Option[String], content: String, default: Boolean): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        val landingPageOption = PersistenceSchema.landingPages.filter(_.id === pageId).firstOption
        if (landingPageOption.isDefined) {
          val uniqueName = PersistenceSchema.landingPages.filter(_.name === name).firstOption
          val landingPage = landingPageOption.get

          if (!name.isEmpty && landingPage.name != name && !uniqueName.isDefined) {
            val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.name)
            q.update(name)
          }

          if (content != landingPage.content) {
            val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.content)
            q.update(content)
          }

          if (!landingPage.default && default) {
            var q = for {l <- PersistenceSchema.landingPages if l.default === true} yield (l.default)
            q.update(false)

            q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.default)
            q.update(default)
          }

          val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.description)
          q.update(description)
        }
      }
    }
  }

  /**
   * Deletes landing page with specified id
   */
  def deleteLandingPage(pageId: Long) = Future[Unit] {
    Future {
      DB.withSession { implicit session =>
        PersistenceSchema.landingPages.filter(_.id === pageId).filter(_.default === false).delete
      }
    }
  }

}
