package managers

import scala.slick.driver.MySQLDriver.simple._
import play.api.libs.concurrent.Execution.Implicits._

import models._
import persistence.db.PersistenceSchema
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object LegalNoticeManager extends BaseManager {
  def logger = LoggerFactory.getLogger("LegalNoticeManager")

  /**
   * Checks if name exists
   */
  def checkUniqueName(name: String): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.legalNotices.filter(_.name === name).firstOption
        !firstOption.isDefined
      }
    }
  }

  /**
   * Checks if name exists except own name (used for update)
   */
  def checkUniqueName(name: String, noticeId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val page = PersistenceSchema.legalNotices.filter(_.id === noticeId).firstOption
        if (page.isDefined) {
          val firstOption = PersistenceSchema.legalNotices.filter(_.id =!= noticeId).filter(_.name === name).firstOption
          !firstOption.isDefined
        } else {
          throw new IllegalArgumentException("Legal notice with ID '" + noticeId + "' not found")
        }
      }
    }
  }

  /**
   * Checks if legal notice exists
   */
  def checkLegalNoticeExists(noticeId: Long): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.legalNotices.filter(_.id === noticeId).firstOption
        firstOption.isDefined
      }
    }
  }

  /**
   * Checks if default legal notice exists
   */
  def checkDefaultLegalNoticeExists(): Future[Boolean] = {
    Future {
      DB.withSession { implicit session =>
        val firstOption = PersistenceSchema.legalNotices.filter(_.default === true).firstOption
        firstOption.isDefined
      }
    }
  }

  /**
   * Gets all legal notices
   */
  def getLegalNotices(): Future[List[LegalNotices]] = {
    Future {
      DB.withSession { implicit session =>
        val pages = PersistenceSchema.legalNotices.list
        pages
      }
    }
  }

  /**
   * Gets legal notice with specified id
   */
  def getLegalNoticeById(noticeId: Long): Future[LegalNotice] = {
    Future {
      DB.withSession { implicit session =>
        val l = PersistenceSchema.legalNotices.filter(_.id === noticeId).firstOption.get
        val ln = new LegalNotice(l)
        ln
      }
    }
  }

  /**
   * Gets the default legal notice
   */
  def getDefaultLegalNotice(): Future[LegalNotice] = {
    Future {
      DB.withSession { implicit session =>
        var notice: LegalNotice = null
        val ln = PersistenceSchema.legalNotices.filter(_.default === true).firstOption
        if (ln.isDefined) {
          notice = new LegalNotice(ln.get)
        }
        notice
      }
    }
  }

  /**
   * Creates new legal notice
   */
  def createLegalNotice(legalNotice: LegalNotices): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        if (legalNotice.default) {
          val q = for {l <- PersistenceSchema.legalNotices if l.default === true} yield (l.default)
          q.update(false)
        }

        PersistenceSchema.insertLegalNotice += legalNotice
      }
    }
  }

  /**
   * Updates legal notice
   */
  def updateLegalNotice(noticeId: Long, name: String, description: Option[String], content: String, default: Boolean): Future[Unit] = {
    Future {
      DB.withSession { implicit session =>
        val legalNoticeOption = PersistenceSchema.legalNotices.filter(_.id === noticeId).firstOption
        if (legalNoticeOption.isDefined) {
          val legalNotice = legalNoticeOption.get

          if (!name.isEmpty && legalNotice.name != name) {
            val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.name)
            q.update(name)
          }

          if (content != legalNotice.content) {
            val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.content)
            q.update(content)
          }

          if (!legalNotice.default && default) {
            var q = for {l <- PersistenceSchema.legalNotices if l.default === true} yield (l.default)
            q.update(false)

            q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.default)
            q.update(default)
          }

          val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.description)
          q.update(description)
        }
      }
    }
  }

  /**
   * Deletes legal notice with specified id
   */
  def deleteLegalNotice(pageId: Long) = Future[Unit] {
    Future {
      DB.withSession { implicit session =>
        PersistenceSchema.legalNotices.filter(_.id === pageId).filter(_.default === false).delete
      }
    }
  }

}
