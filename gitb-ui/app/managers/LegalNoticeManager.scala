package managers

import javax.inject.{Inject, Singleton}
import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class LegalNoticeManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def logger = LoggerFactory.getLogger("LegalNoticeManager")

  /**
   * Gets all legal notices for the specified community
   */
  def getLegalNoticesByCommunity(communityId: Long): List[LegalNotices] = {
    val legalNotices = exec(PersistenceSchema.legalNotices.filter(_.community === communityId)
        .sortBy(_.name.asc)
      .result.map(_.toList))
    legalNotices
  }

  /**
   * Checks if name exists
   */
  def checkUniqueName(name: String, communityId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.legalNotices.filter(_.community === communityId).filter(_.name === name).result.headOption)
    firstOption.isEmpty
  }

  /**
    * Checks if a legal notice with given name exists for the given community
    */
  def checkUniqueName(noticeId: Long, name: String, communityId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.legalNotices.filter(_.community === communityId).filter(_.id =!= noticeId).filter(_.name === name).result.headOption)
    firstOption.isEmpty
  }

  /**
   * Gets legal notice with specified id
   */
  def getLegalNoticeById(noticeId: Long): LegalNotice = {
    val l = exec(PersistenceSchema.legalNotices.filter(_.id === noticeId).result.head)
    val ln = new LegalNotice(l)
    ln
  }

  def getCommunityId(noticeId: Long): Long = {
    exec(PersistenceSchema.legalNotices.filter(_.id === noticeId).map(x => x.community).result.head)
  }

  /**
   * Creates new legal notice
   */
  def createLegalNotice(legalNotice: LegalNotices) = {
    exec(createLegalNoticeInternal(legalNotice).transactionally)
  }

  def createLegalNoticeInternal(legalNotice: LegalNotices) = {
    for {
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (legalNotice.default) {
          val q = for {l <- PersistenceSchema.legalNotices if l.default === true && l.community === legalNotice.community} yield (l.default)
          actions += q.update(false)
        }
        toDBIO(actions)
      }
      newId <- PersistenceSchema.insertLegalNotice += legalNotice
    } yield newId
  }

  /**
   * Updates legal notice
   */
  def updateLegalNotice(noticeId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    exec(updateLegalNoticeInternal(noticeId, name, description, content, default, communityId).transactionally)
  }

  def updateLegalNoticeInternal(noticeId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    for {
      legalNoticeOption <- PersistenceSchema.legalNotices.filter(_.id === noticeId).result.headOption
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (legalNoticeOption.isDefined) {
          val legalNotice = legalNoticeOption.get
          if (!name.isEmpty && legalNotice.name != name) {
            val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.name)
            actions += q.update(name)
          }
          if (content != legalNotice.content) {
            val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.content)
            actions += q.update(content)
          }

          if (!legalNotice.default && default) {
            var q = for {l <- PersistenceSchema.legalNotices if l.default === true && l.community === communityId} yield (l.default)
            actions += q.update(false)

            q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.default)
            actions += q.update(default)
          }

          val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield (l.description)
          actions += q.update(description)
        }
        toDBIO(actions)
      }
    } yield ()
  }

  /**
   * Deletes legal notice with specified id
   */
  def deleteLegalNotice(noticeId: Long) = {
    exec(deleteLegalNoticeInternal(noticeId).transactionally)
  }

  def deleteLegalNoticeInternal(noticeId: Long): DBIO[_] = {
    PersistenceSchema.legalNotices.filter(_.id === noticeId).delete
  }

  /**
   * Gets the default legal notice for given community
   */
  def getCommunityDefaultLegalNotice(communityId: Long): Option[LegalNotice] = {
    val n = exec(PersistenceSchema.legalNotices.filter(_.community === communityId).filter(_.default === true).result.headOption)
    val defaultLegalNotice = n match {
      case Some(n) => Some(new LegalNotice(n))
      case None => None
    }
    defaultLegalNotice
  }

  def deleteLegalNoticeByCommunity(communityId: Long) = {
    PersistenceSchema.legalNotices.filter(_.community === communityId).delete
  }

}