package managers

import javax.inject.{Inject, Singleton}
import models._
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class LegalNoticeManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  private var globalDefaultLegalNotice: Option[Option[LegalNotices]] = None

  /**
   * Gets all landing pages for the specified community without rich content
   */
  def getLegalNoticesByCommunityWithoutContent(communityId: Long): List[LegalNotice] = {
    exec(
      PersistenceSchema.legalNotices
        .filter(_.community === communityId)
        .map(x => (x.id, x.name, x.description, x.default))
        .sortBy(_._2.asc)
        .result
        .map(_.toList.map(x => new LegalNotice(x._1, x._2, x._3, None, x._4)))
    )
  }

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
  def getLegalNoticeById(noticeId: Long): Option[LegalNotices] = {
    exec(getLegalNoticeByIdInternal(noticeId))
  }

  def getLegalNoticeByIdInternal(noticeId: Long): DBIO[Option[LegalNotices]] = {
    PersistenceSchema.legalNotices.filter(_.id === noticeId).result.headOption
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
          val q = for {l <- PersistenceSchema.legalNotices if l.default === true && l.community === legalNotice.community} yield l.default
          actions += q.update(false)
          // Remove global legal notice from the cache
          if (legalNotice.default && legalNotice.community == Constants.DefaultCommunityId) {
            globalDefaultLegalNotice = None
          }
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
          if (name.nonEmpty && legalNotice.name != name) {
            val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield l.name
            actions += q.update(name)
          }
          if (content != legalNotice.content) {
            val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield l.content
            actions += q.update(content)
          }

          if (!legalNotice.default && default) {
            var q = for {l <- PersistenceSchema.legalNotices if l.default === true && l.community === communityId} yield l.default
            actions += q.update(false)

            q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield l.default
            actions += q.update(default)

          }

          val q = for {l <- PersistenceSchema.legalNotices if l.id === noticeId} yield l.description
          actions += q.update(description)
        }
        // Remove global default notice from cache
        if (communityId == Constants.DefaultCommunityId) {
          globalDefaultLegalNotice = None
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
    for {
      noticeInfo <- PersistenceSchema.legalNotices.filter(_.id === noticeId).map(x => (x.community, x.default)).result.headOption
      _ <- PersistenceSchema.organizations.filter(_.legalNotice === noticeId).map(_.legalNotice).update(None)
      _ <- PersistenceSchema.legalNotices.filter(_.id === noticeId).delete
      _ <- {
        if (noticeInfo.isDefined && noticeInfo.get._1 == Constants.DefaultCommunityId && noticeInfo.get._2) {
          // Remove the cached global legal notice
          globalDefaultLegalNotice = None
        }
        DBIO.successful(())
      }
    } yield ()
  }

  /**
   * Gets the default legal notice for given community
   */
  def getCommunityDefaultLegalNotice(communityId: Long): Option[LegalNotices] = {
    if (communityId == Constants.DefaultCommunityId && globalDefaultLegalNotice.isDefined) {
        globalDefaultLegalNotice.get
    } else {
      val defaultLegalNotice = exec(PersistenceSchema.legalNotices.filter(_.community === communityId).filter(_.default === true).result.headOption)
      if (communityId == Constants.DefaultCommunityId) {
        globalDefaultLegalNotice = Some(defaultLegalNotice)
      }
      defaultLegalNotice
    }
  }

  def communityHasDefaultLegalNotice(communityId: Long): DBIO[Boolean] = {
    if (communityId == Constants.DefaultCommunityId && globalDefaultLegalNotice.flatten.isDefined) {
      DBIO.successful(true)
    } else {
      PersistenceSchema.legalNotices.filter(_.community === communityId).filter(_.default === true).exists.result
    }
  }

  def deleteLegalNoticeByCommunity(communityId: Long) = {
    for {
      _ <- PersistenceSchema.legalNotices.filter(_.community === communityId).delete
      _ <- {
        if (communityId == Constants.DefaultCommunityId) {
          // Remove the cached global legal notice
          globalDefaultLegalNotice = None
        }
        DBIO.successful(())
      }
    } yield ()
  }

}