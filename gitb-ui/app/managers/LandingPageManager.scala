package managers

import javax.inject.{Inject, Singleton}
import models._
import persistence.db.PersistenceSchema
import play.api.db.slick.DatabaseConfigProvider

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by VWYNGAET on 25/11/2016.
 */
@Singleton
class LandingPageManager @Inject() (dbConfigProvider: DatabaseConfigProvider) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  /**
   * Gets all landing pages for the specified community without rich content
   */
  def getLandingPagesByCommunityWithoutContent(communityId: Long): List[LandingPage] = {
    exec(
      PersistenceSchema.landingPages
        .filter(_.community === communityId)
        .map(x => (x.id, x.name, x.description, x.default))
        .sortBy(_._2.asc)
        .result
        .map(_.toList.map(x => new LandingPage(x._1, x._2, x._3, None, x._4)))
    )
  }

  /**
   * Gets all landing pages for the specified community
   */
  def getLandingPagesByCommunity(communityId: Long): List[LandingPages] = {
    val pages = exec(PersistenceSchema.landingPages.filter(_.community === communityId)
        .sortBy(_.name.asc)
      .result.map(_.toList))
    pages
  }

  /**
   * Checks if a landing page with given name exists for the given community
   */
  def checkUniqueName(name: String, communityId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.landingPages.filter(_.community === communityId).filter(_.name === name).result.headOption)
    firstOption.isEmpty
  }

  /**
    * Creates new landing page
    */
  def createLandingPage(landingPage: LandingPages) = {
    exec(createLandingPageInternal(landingPage).transactionally)
  }

  def createLandingPageInternal(landingPage: LandingPages): DBIO[Long] = {
    for {
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (landingPage.default) {
          val q = for {l <- PersistenceSchema.landingPages if l.default === true && l.community === landingPage.community} yield l.default
          actions += q.update(false)
        }
        toDBIO(actions)
      }
      newId <- PersistenceSchema.insertLandingPage += landingPage
    } yield newId

  }

  /**
    * Gets landing page with specified id
    */
  def getLandingPageById(pageId: Long): Option[LandingPages] = {
    exec(getLandingPageByIdInternal(pageId))
  }

  def getLandingPageByIdInternal(pageId: Long): DBIO[Option[LandingPages]] = {
    PersistenceSchema.landingPages.filter(_.id === pageId).result.headOption
  }

  def getCommunityId(pageId: Long): Long = {
    exec(PersistenceSchema.landingPages.filter(_.id === pageId).map(x => x.community).result.head)
  }

  /**
   * Checks if a landing page with given name exists for the given community
   */
  def checkUniqueName(pageId: Long, name: String, communityId: Long): Boolean = {
    val firstOption = exec(PersistenceSchema.landingPages.filter(_.community === communityId).filter(_.id =!= pageId).filter(_.name === name).result.headOption)
    firstOption.isEmpty
  }

  /**
   * Updates landing page
   */
  def updateLandingPage(pageId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    exec(updateLandingPageInternal(pageId, name, description, content, default, communityId).transactionally)
  }

  def updateLandingPageInternal(pageId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    for {
      landingPageOption <- PersistenceSchema.landingPages.filter(_.id === pageId).result.headOption
      _ <- {
        val actions = new ListBuffer[DBIO[_]]()
        if (landingPageOption.isDefined) {
          val landingPage = landingPageOption.get
          if (!name.isEmpty && landingPage.name != name) {
            val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.name)
            actions += q.update(name)
          }

          if (content != landingPage.content) {
            val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.content)
            actions += q.update(content)
          }

          if (!landingPage.default && default) {
            var q = for {l <- PersistenceSchema.landingPages if l.default === true && l.community === communityId} yield (l.default)
            actions += q.update(false)

            q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.default)
            actions += q.update(default)
          }

          val q = for {l <- PersistenceSchema.landingPages if l.id === pageId} yield (l.description)
          actions += q.update(description)
        }
        toDBIO(actions)
      }
    } yield ()
  }

  /**
   * Deletes landing page with specified id
   */
  def deleteLandingPage(pageId: Long) = {
    exec(deleteLandingPageInternal(pageId).transactionally)
  }

  def deleteLandingPageInternal(pageId: Long): DBIO[_] = {
    for {
      _ <- {
        (for {
          x <- PersistenceSchema.organizations if x.landingPage === pageId
        } yield x.landingPage).update(None)

      }
      _ <- PersistenceSchema.landingPages.filter(_.id === pageId).delete
    } yield ()
  }

  /**
    * Gets the default landing page for given community
    */
  def getCommunityDefaultLandingPage(communityId: Long): Option[LandingPages] = {
    exec(getCommunityDefaultLandingPageInternal(communityId))
  }

  def getCommunityDefaultLandingPageInternal(communityId: Long): DBIO[Option[LandingPages]] = {
    for {
      communityLandingPage <- PersistenceSchema.landingPages.filter(_.community === communityId).filter(_.default === true).result.headOption
      landingPageToUse <- {
        if (communityLandingPage.isDefined) {
          DBIO.successful(communityLandingPage)
        } else {
          // Test Bed default landing page.
          PersistenceSchema.landingPages.filter(_.community === Constants.DefaultCommunityId).filter(_.default === true).result.headOption
        }
      }
    } yield landingPageToUse
  }

  def deleteLandingPageByCommunity(communityId: Long) = {
    PersistenceSchema.landingPages.filter(_.community === communityId).delete
  }

}