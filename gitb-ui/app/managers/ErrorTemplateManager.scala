package managers

import models._
import org.slf4j.LoggerFactory
import persistence.db.PersistenceSchema

import scala.slick.driver.MySQLDriver.simple._

object ErrorTemplateManager extends BaseManager {
  def logger = LoggerFactory.getLogger("ErrorTemplateManager")

  /**
   * Gets all error templates for the specified community
   */
  def getErrorTemplatesByCommunity(communityId: Long): List[ErrorTemplates] = {
    DB.withSession { implicit session =>
      val errorTemplates = PersistenceSchema.errorTemplates.filter(_.community === communityId)
          .sortBy(_.name.asc)
        .list
      errorTemplates
    }
  }

  /**
   * Checks if name exists
   */
  def checkUniqueName(name: String, communityId: Long): Boolean = {
    DB.withSession { implicit session =>
      val firstOption = PersistenceSchema.errorTemplates.filter(_.community === communityId).filter(_.name === name).firstOption
      firstOption.isEmpty
    }
  }

  /**
    * Checks if a error template with given name exists for the given community
    */
  def checkUniqueName(templateId: Long, name: String, communityId: Long): Boolean = {
    DB.withSession { implicit session =>
      val firstOption = PersistenceSchema.errorTemplates.filter(_.community === communityId).filter(_.id =!= templateId).filter(_.name === name).firstOption
      firstOption.isEmpty
    }
  }

  /**
   * Gets error template with specified id
   */
  def getErrorTemplateById(templateId: Long): ErrorTemplate = {
    DB.withSession { implicit session =>
      val r = PersistenceSchema.errorTemplates.filter(_.id === templateId).firstOption.get
      val e = new ErrorTemplate(r)
      e
    }
  }

  /**
   * Creates new error template
   */
  def createErrorTemplate(errorTemplate: ErrorTemplates) = {
    DB.withTransaction { implicit session =>
      if (errorTemplate.default) {
        val q = for {l <- PersistenceSchema.errorTemplates if l.default === true && l.community === errorTemplate.community} yield (l.default)
        q.update(false)
      }

      PersistenceSchema.insertErrorTemplate += errorTemplate
    }
  }

  /**
   * Updates error template
   */
  def updateErrorTemplate(templateId: Long, name: String, description: Option[String], content: String, default: Boolean, communityId: Long) = {
    DB.withTransaction { implicit session =>
      val errorTemplateOption = PersistenceSchema.errorTemplates.filter(_.id === templateId).firstOption
      if (errorTemplateOption.isDefined) {
        val errorTemplate = errorTemplateOption.get

        if (!name.isEmpty && errorTemplate.name != name) {
          val q = for {l <- PersistenceSchema.errorTemplates if l.id === templateId} yield (l.name)
          q.update(name)
        }

        if (content != errorTemplate.content) {
          val q = for {l <- PersistenceSchema.errorTemplates if l.id === templateId} yield (l.content)
          q.update(content)
        }

        if (!errorTemplate.default && default) {
          var q = for {l <- PersistenceSchema.errorTemplates if l.default === true && l.community === communityId} yield (l.default)
          q.update(false)

          q = for {l <- PersistenceSchema.errorTemplates if l.id === templateId} yield (l.default)
          q.update(default)
        }

        val q = for {l <- PersistenceSchema.errorTemplates if l.id === templateId} yield (l.description)
        q.update(description)
      }
    }
  }

  /**
   * Deletes error template with specified id
   */
  def deleteErrorTemplate(templateId: Long) = {
    DB.withTransaction { implicit session =>
      PersistenceSchema.errorTemplates.filter(_.id === templateId).delete
    }
  }


  /**
   * Gets the default error template for given community
   */
  def getCommunityDefaultErrorTemplate(communityId: Long): ErrorTemplate = {
    DB.withSession { implicit session =>
      val n = PersistenceSchema.errorTemplates.filter(_.community === communityId).filter(_.default === true).firstOption
      val defaultErrorTemplate = n match {
        case Some(n) => new ErrorTemplate(n)
        case None => null
      }
      defaultErrorTemplate
    }
  }

  def deleteErrorTemplateByCommunity(communityId: Long)(implicit session: Session) = {
    PersistenceSchema.errorTemplates.filter(_.community === communityId).delete
  }

}