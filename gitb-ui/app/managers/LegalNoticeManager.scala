package managers

import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

object LegalNoticeManager extends BaseManager {
  def logger = LoggerFactory.getLogger("LegalNoticeManager")

  /**
   * Gets all legal notices for the specified community
   */
  def getLegalNoticesByCommunity(communityId: Long): List[LegalNotices] = {
    DB.withSession { implicit session =>
      val legalNotices = PersistenceSchema.legalNotices.filter(_.community === communityId).list
      legalNotices
    }
  }

  /**
   * Checks if name exists
   */
  def checkUniqueName(name: String, communityId: Long): Boolean = {
    DB.withSession { implicit session =>
      val firstOption = PersistenceSchema.legalNotices.filter(_.community === communityId).filter(_.name === name).firstOption
      firstOption.isEmpty
    }
  }

  /**
    * Checks if a legal notice with given name exists for the given community
    */
  def checkUniqueName(noticeId: Long, name: String, communityId: Long): Boolean = {
    DB.withSession { implicit session =>
      val firstOption = PersistenceSchema.legalNotices.filter(_.community === communityId).filter(_.id =!= noticeId).filter(_.name === name).firstOption
      firstOption.isEmpty
    }
  }

  /**
   * Gets legal notice with specified id
   */
  def getLegalNoticeById(noticeId: Long): LegalNotice = {
    DB.withSession { implicit session =>
      val l = PersistenceSchema.legalNotices.filter(_.id === noticeId).firstOption.get
      val ln = new LegalNotice(l)
      ln
    }
  }

  /**
   * Creates new legal notice
   */
  def createLegalNotice(legalNotice: LegalNotices) = {
    DB.withTransaction { implicit session =>
      if (legalNotice.default) {
        val q = for {l <- PersistenceSchema.legalNotices if l.default === true && l.community === legalNotice.community} yield (l.default)
        q.update(false)
      }

      PersistenceSchema.insertLegalNotice += legalNotice
    }
  }

  /**
   * Updates legal notice
   */
  def updateLegalNotice(noticeId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    DB.withTransaction { implicit session =>
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
          var q = for {l <- PersistenceSchema.legalNotices if l.default === true && l.community === communityId} yield (l.default)
          q.update(false)

          q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.default)
          q.update(default)
        }

        val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.description)
        q.update(description)
      }
    }
  }

  /**
   * Deletes legal notice with specified id
   */
  def deleteLegalNotice(pageId: Long) = {
    DB.withTransaction { implicit session =>
      PersistenceSchema.legalNotices.filter(_.id === pageId).delete
    }
  }


  /**
   * Gets the default legal notice for given community
   */
  def getCommunityDefaultLegalNotice(communityId: Long): LegalNotice = {
    DB.withSession { implicit session =>
      val n = PersistenceSchema.legalNotices.filter(_.community === communityId).filter(_.default === true).firstOption
      val defaultLegalNotice = n match {
        case Some(n) => new LegalNotice(n)
        case None => null
      }
      defaultLegalNotice
    }
  }

  def deleteLegalNoticeByCommunity(communityId: Long)(implicit session: Session) = {
    PersistenceSchema.legalNotices.filter(_.community === communityId).delete
  }

}